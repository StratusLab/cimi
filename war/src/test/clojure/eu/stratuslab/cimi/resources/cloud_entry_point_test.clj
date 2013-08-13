(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]
    [peridot.core :refer :all]))

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(deftest check-strip-unknown-attributes
  (let [input {:a 1 :b 2 :id "ok"}
        correct {:id "ok"}]
    (is (= correct (strip-unknown-attributes input)))))

(deftest check-strip-immutable-attributes
  (let [input {:a 1 :id "ok" :baseURI "ok"}
        correct {:a 1}]
    (is (= correct (strip-immutable-attributes input))))
  (let [input {:name 1 :id "ok" :baseURI "ok"}
        correct {:name 1}]
    (is (= correct (strip-immutable-attributes input)))))

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
