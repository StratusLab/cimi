(ns eu.stratuslab.cimi.resources.service-message-test
  (:require
    [eu.stratuslab.cimi.resources.service-message :refer :all]
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
  {:name "title"
   :description "description"})

(deftest test-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        uri (uuid->uri timestamp)
        service-message {:id uri
                         :resourceURI type-uri
                         :created timestamp
                         :updated timestamp
                         :name "title"
                         :description "description"}]
    (is (empty? (validation-errors ServiceMessage service-message)))
    (is (not (empty? (validation-errors ServiceMessage (dissoc service-message :name)))))
    (is (not (empty? (validation-errors ServiceMessage (dissoc service-message :description)))))))

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
        (let [uuid (second (re-matches #"ServiceMessage/(.*)" uri))]
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
                    entries (:serviceMessages body)
                    ids (set (map :id entries))]
                (is (= collection-type-uri resource-uri))
                (is (pos? (:count body)))
                (is (= (count entries) (:count body)))
                (is (ids uri))))

            ;; update the resource
            (let [current (retrieve t/*test-cb-client* uuid)
                  updated (assoc (:body current) :name "NEW TITLE")
                  updated-resp (edit t/*test-cb-client* uuid updated)]
              (is (rresp/response? updated-resp))
              (is (= 200 (:status updated-resp))))

            ;; get the resource again and make sure it's updated
            (let [resp (retrieve t/*test-cb-client* uuid)]
              (is (rresp/response? resp))
              (is (= 200 (:status resp)))
              (is (= "NEW TITLE" (:name (:body resp)))))

            ;; delete the resource
            (let [resp (delete t/*test-cb-client* uuid)]
              (is (rresp/response? resp))
              (is (= 200 (:status resp))))

            ;; ensure that resource is gone
            (let [resp (retrieve t/*test-cb-client* uuid)]
              (is (rresp/response? resp))
              (is (= 404 (:status resp))))))))))
