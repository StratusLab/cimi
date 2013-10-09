(ns eu.stratuslab.cimi.resources.volume-template-test
  (:require
   [eu.stratuslab.cimi.resources.volume-template :refer :all]
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
  {:volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage {:href "VolumeImage/mkplaceid"}})

(deftest test-volume-template-schema
  (let [uri (uuid->uri (utils/create-uuid))
        volume-template (assoc valid-entry
             :id uri
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]
        (is (empty? (validation-errors VolumeTemplate volume-template)))
        (is (not (empty? (validation-errors VolumeTemplate (dissoc volume-template :volumeConfig)))))
        (is (empty? (validation-errors VolumeTemplate (dissoc volume-template :volumeImage))))))

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
              (println resp)
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
