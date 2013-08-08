(ns eu.stratuslab.cimi.resources.cloud-entry-point-resources
  (:use clojure.test
        peridot.core)
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as cb-utils]
    [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]))

(use-fixtures :once cb-utils/temp-bucket-fixture)

(defn ring-app [cb-client]
  (bootstrap cb-client)
  (wrap-cb-client cb-client resource-routes))

(deftest retrieve-cloud-entry-point
  (let [results (-> (session (ring-app cb-utils/*test-cb-client*))
                  (request "/"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resource-type body) resource-type))
    (is (= (:resourceURI body) resource-uri))))

(deftest update-cloud-entry-point
  (let [results (-> (session (ring-app cb-utils/*test-cb-client*))
                  (content-type "application/json")
                  (request "/"
                    :request-method :put
                    :body "{\"name\": \"dummy\"}"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (empty? body)))
  (let [results (-> (session (ring-app cb-utils/*test-cb-client*))
                  (request "/"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resource-type body) resource-type))
    (is (= (:resourceURI body) resource-uri))
    (is (= (:name body) "dummy"))))

(deftest delete-cloud-entry-point
  (let [results (-> (session (ring-app cb-utils/*test-cb-client*))
                  (request "/" :request-method :delete))
        response (:response results)]
    (is (nil? response))))
