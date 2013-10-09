(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.test :refer :all]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(deftest lifecycle

  ;; retrieve cloud entry point anonymously
  (let [results (-> (session (ring-app))
                  (request "/"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (is (= (:status response) 200))
    (is (= (:resourceURI body) type-uri)))
  
  ;; update the entry, verify updated doc is returned
  ;; must be done as administrator
  (let [results (-> (session (ring-app))
                  (authorize "root" "admin_password")
                  (content-type "application/json")
                  (request "/"
                    :request-method :put
                    :body "{\"name\": \"dummy\"}"))
        response (:response results)
        body (:body response)
        request (:request results)]
    (println request)
    (println response)
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
    (is (= (:name body) "dummy")))

  ;; verify that the delete fails
  (let [results (-> (session (ring-app))
                  (request "/" :request-method :delete))
        response (:response results)]
    (is (nil? response))))
