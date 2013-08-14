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
    (is (= (:resourceURI body) type-uri))))

(deftest update-cloud-entry-point

  ;; update the entry, verify updated doc is returned
  (let [results (-> (session (ring-app))
                  (content-type "application/json")
                  (request "/"
                    :request-method :put
                    :body "{\"name\": \"dummy\"}"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resourceURI body) type-uri))
    (is (= (:name body) "dummy")))

  ;; verify that subsequent reads find the right data
  (let [results (-> (session (ring-app))
                  (request "/"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resourceURI body) type-uri))
    (is (= (:name body) "dummy"))))

(deftest delete-cloud-entry-point
  (let [results (-> (session (ring-app))
                  (request "/" :request-method :delete))
        response (:response results)]
    (is (nil? response))))
