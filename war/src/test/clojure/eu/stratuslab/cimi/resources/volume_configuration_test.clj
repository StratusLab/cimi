(ns eu.stratuslab.cimi.resources.volume-configuration-test
  (:require
    [eu.stratuslab.cimi.resources.volume-configuration :refer :all]
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
  {:type "http://stratuslab.eu/cimi/1/raw"
   :format "ext4"
   :capacity 1000})

(deftest test-volume-configuration-schema
  (let [uri (uuid->uri (utils/create-uuid))
        volume-configuration (assoc valid-entry
                               :id uri
                               :resourceURI type-uri
                               :created "1964-08-25T10:00:00.0Z"
                               :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors VolumeConfiguration volume-configuration)))
    (is (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :type))))
    (is (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :format))))
    (is (not (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :capacity)))))))

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
        (let [uuid (second (re-matches #"VolumeConfiguration/(.*)" uri))]
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
                    entries (:volumeConfigurations body)
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
