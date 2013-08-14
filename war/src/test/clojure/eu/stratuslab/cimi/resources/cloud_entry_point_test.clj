(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]
    [peridot.core :refer :all]))

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

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
