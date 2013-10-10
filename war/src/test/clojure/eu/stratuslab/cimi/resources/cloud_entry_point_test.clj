(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.test :refer :all]
    [clojure.pprint :refer [pprint]]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(deftest lifecycle

  ;; retrieve cloud entry point anonymously
  (-> (session (ring-app))
      (request "/")
      (t/is-status 200)
      (t/is-resource-uri type-uri))

  ;; update the entry, verify updated doc is returned
  ;; must be done as administrator
  (-> (session (ring-app))
      (authorize "root" "admin_password")
      (content-type "application/json")
      (request "/"
               :request-method :put
               :body "{\"name\": \"dummy\"}")
      (t/is-status 200)
      (t/is-resource-uri type-uri)
      (t/is-key-value :name "dummy"))

  ;; verify that subsequent reads find the right data
  (-> (session (ring-app))
      (request "/")
      (t/is-status 200)
      (t/is-resource-uri type-uri)
      (t/is-key-value :name "dummy"))

  ;; verify that delete action is not treated
  (-> (session (ring-app))
      (request "/" :request-method :delete)
      (t/is-nil-response)))
