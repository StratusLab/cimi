(ns eu.stratuslab.cimi.resources.volume-image-test
  (:require
   [eu.stratuslab.cimi.resources.volume-image :refer :all]
   [eu.stratuslab.cimi.resources.utils :as utils]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [clj-schema.validation :refer [validation-errors]]
   [ring.util.response :as rresp]
   [clojure.test :refer :all]
   [clojure.data.json :as json]
   [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:state "CREATING"
   :imageLocation {:href "GWE_nifKGCcXiFk42XaLrS8LQ-J"}
   :bootable true} )

(deftest test-image-id-check 
  (let [id "GWE_nifKGCcXiFk42XaLrS8LQ-J"]
    (is (= id (image-id {:imageLocation {:href id}})))
    (is (nil? (image-id {:imageLocation {:href "BAD"}})))
    (is (nil? (image-id {})))))

(deftest test-volume-image-schema
  (let [volume-image (assoc valid-entry
             :id "VolumeImage/10"
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]
        (is (empty? (validation-errors VolumeImage volume-image)))
        (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :state)))))
        (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :imageLocation)))))
        (is (not (empty? (validation-errors VolumeImage (assoc volume-image :imageLocation {})))))
        (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :bootable)))))))

(deftest lifecycle 
  ;; create resource
  (let [resp (add t/*test-cb-client* valid-entry)]
    (is (rresp/response? resp))
    (is (= 201 (:status resp)))
    (let [headers (:headers resp)]
      (is headers)
      (let [uri (get headers "Location")]
        (is uri)
        
        ;; get uri and retrieve resource
        (let [uuid (second (re-matches #"VolumeImage/(.*)" uri))]
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
                    entries (:volumeImages body)
                    ids (set (map :id entries))]
                (is (= collection-type-uri resource-uri))
                (is (pos? (:count body)))
                (is (= (count entries) (:count body)))
                (is (ids uri))))
            
            ;; delete the resource
            ;; this is an asynchronous request and should produce a job
            ;; there no daemon to service jobs, so don't check if it's disappeared
            (let [resp (delete t/*test-cb-client* uuid)
                  job-uri (get-in resp [:headers "CIMI-Job-URI"])]
              (is (rresp/response? resp))
              (is (= 202 (:status resp)))
              (is (.startsWith job-uri "Job/")))))))))
