(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.test :refer :all]
    [peridot.core :refer :all]))

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(deftest test-resource-link
  (let [ref {:href "https://example.org/resource"}]
    (is (empty? (validation-errors ResourceLink ref)))
    (is (not (empty? (validation-errors ResourceLink (dissoc ref :href)))))
    (is (not (empty? (validation-errors ResourceLink (assoc ref :bad "BAD")))))))

(deftest retrieve-cloud-entry-point
  (let [results (-> (session (ring-app))
                  (request "/"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resourceURI body) resource-uri))))

(deftest update-cloud-entry-point
  (let [results (-> (session (ring-app))
                  (content-type "application/json")
                  (request "/"
                    :request-method :put
                    :body "{\"name\": \"dummy\"}"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (empty? body)))
  (let [results (-> (session (ring-app))
                  (request "/"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resourceURI body) resource-uri))
    (is (= (:name body) "dummy"))))

(deftest delete-cloud-entry-point
  (let [results (-> (session (ring-app))
                  (request "/" :request-method :delete))
        response (:response results)]
    (is (nil? response))))
