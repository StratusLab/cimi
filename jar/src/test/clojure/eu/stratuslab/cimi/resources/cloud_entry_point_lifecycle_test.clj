(ns eu.stratuslab.cimi.resources.cloud-entry-point-lifecycle-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [eu.stratuslab.cimi.app.routes :as routes]))

(use-fixtures :once t/temp-bucket-fixture)

(use-fixtures :each t/flush-bucket-fixture)

(defn ring-app []
  (t/make-ring-app (t/concat-routes [routes])))

(deftest lifecycle

  ;; initialize the cloud entry point
  (add)

  ;; retrieve cloud entry point anonymously
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-absent "edit"))

  ;; updating CEP as user should fail
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (content-type "application/json")
      (request base-uri
               :request-method :put
               :body (json/write-str {:name "dummy"}))
      (t/is-status 403))

  ;; retrieve cloud entry point as root
  (-> (session (ring-app))
      (authorize "root" "admin_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-present "edit"))

  ;; update the entry, verify updated doc is returned
  ;; must be done as administrator
  (-> (session (ring-app))
      (authorize "root" "admin_password")
      (content-type "application/json")
      (request base-uri
               :request-method :put
               :body (json/write-str {:name "dummy"}))
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-present "edit")
      (t/is-key-value :name "dummy"))

  ;; verify that subsequent reads find the right data
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri resource-uri)
      (t/is-operation-absent "edit")
      (t/is-key-value :name "dummy")))

(deftest bad-methods
  (doall
    (for [[uri method] [[base-uri :options]
                        [base-uri :delete]
                        [base-uri :post]]]
      (do
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (t/is-status 405))))))
