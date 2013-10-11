(ns eu.stratuslab.cimi.resources.volume-test
  (:require
    [eu.stratuslab.cimi.resources.volume :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.validation :refer [validation-errors]]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:state "CREATING"
   :type "http://schemas.cimi.stratuslab.eu/normal"
   :capacity 1024
   :bootable true
   :eventLog "EventLog/uuid"})

(def valid-template
  {:resourceURI "http://schemas.dmtf.org/cimi/1/VolumeCreate"
   :name "template"
   :description "dummy template"
   :volumeTemplate {:volumeConfig {:type "http://schemas.cimi.stratuslab.eu/normal"
                                   :format "ext4"
                                   :capacity 1024}
                    :volumeImage {:state "AVAILABLE"
                                  :imageLocation {:href "https://marketplace.stratuslab.eu/A"}
                                  :bootable true}}})

(deftest test-volume-schema
  (let [volume (assoc valid-entry
                 :id "Volume/10"
                 :resourceURI type-uri
                 :created "1964-08-25T10:00:00.0Z"
                 :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors Volume volume)))
    (is (empty? (validation-errors Volume (dissoc volume :state))))
    (is (empty? (validation-errors Volume (dissoc volume :bootable))))
    (is (empty? (validation-errors Volume (dissoc volume :eventLog))))
    (is (not (empty? (validation-errors Volume (dissoc volume :type)))))
    (is (not (empty? (validation-errors Volume (dissoc volume :capacity)))))))

(deftest lifecycle
  ;; create resource
  (let [resp (add t/*test-cb-client* valid-template)]
    (is (rresp/response? resp))
    (is (= 201 (:status resp)))
    (let [headers (:headers resp)]
      (is headers)
      (let [uri (get headers "Location")
            job-uri (get headers "CIMI-Job-URI")]
        (is uri)
        (is job-uri)
        (is (.startsWith job-uri "Job/"))

        (let [uuid (second (re-matches #"Volume/(.*)" uri))]
          (is uuid)
          (let [resp (retrieve t/*test-cb-client* uuid)]
            (is (rresp/response? resp))
            (is (= 200 (:status resp)))
            (let [body (:body resp)]
              (is body))

            ;; ensure resource is found by query
            (let [resp (query t/*test-cb-client*)]
              (is (rresp/response? resp))
              (is (= 200 (:status resp)))
              (let [body (:body resp)
                    resource-uri (:resourceURI body)
                    entries (:volumes body)
                    ids (set (map :id entries))]
                (is (= collection-type-uri resource-uri))
                (is (pos? (:count body)))
                (is (= (count entries) (:count body)))
                (is (ids uri))))

            ;; delete the resource
            (let [resp (delete t/*test-cb-client* uuid)]
              (is (rresp/response? resp))
              (is (= 202 (:status resp))))

            (let [resp (delete t/*test-cb-client* uuid)
                  job-uri (get-in resp [:headers "CIMI-Job-URI"])]
              (is (rresp/response? resp))
              (is (= 202 (:status resp)))
              (is (.startsWith job-uri "Job/")))))))))

#_(deftest lifecycle 
  ;; create resource
  (let [resp (add t/*test-cb-client* valid-entry)]
    (is (rresp/response? resp))
    (is (= 201 (:status resp)))
    (let [headers (:headers resp)]
      (is headers)
      (let [uri (get headers "Location")]
        (is uri)
        
        ;; get uri and retrieve resource
        (let [uuid (second (re-matches #"VolumeTemplate/(.*)" uri))]
          (is uuid)
          (let [resp (retrieve t/*test-cb-client* uuid)]
            (is (rresp/response? resp))
            (is (= 200 (:status resp)))
            (let [body (:body resp)]
              (is body)
              (is (= body (merge body valid-entry))))
            
            ;; ensure resource is found by query
            (let [resp (query t/*test-cb-client*)]
              (is (rresp/response? resp))
              (is (= 200 (:status resp)))
              (let [body (:body resp)
                    resource-uri (:resourceURI body)
                    entries (:volumeTemplates body)
                    ids (set (map :id entries))]
                (is (= collection-type-uri resource-uri))
                (is (pos? (:count body)))
                (is (= (count entries) (:count body)))
                (is (ids uri))))
            
            ;; delete the resource
            (let [resp (delete t/*test-cb-client* uuid)]
              (is (rresp/response? resp))
              (is (= 200 (:status resp))))
            
            ;; ensure that resource is gone
            (let [resp (retrieve t/*test-cb-client* uuid)]
              (is (rresp/response? resp))
              (is (= 404 (:status resp))))))))))
