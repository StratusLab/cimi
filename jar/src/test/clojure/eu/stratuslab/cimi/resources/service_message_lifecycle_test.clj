(ns eu.stratuslab.cimi.resources.service-message-lifecycle-test
  (:require
    [eu.stratuslab.cimi.resources.service-message :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [eu.stratuslab.cimi.app.routes :as routes]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [eu.stratuslab.cimi.resources.common.schema :as c]))

(use-fixtures :once t/temp-bucket-fixture)

(use-fixtures :each t/flush-bucket-fixture)

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def ^:const base-uri (str c/service-context resource-name))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-entry
  {:acl     valid-acl
   :title   "title"
   :message "description"})

(deftest lifecycle

  ;; anonymous create should fail
  (-> (session (ring-app))
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; user create should also fail
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; anonymous query should succeed
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri collection-uri)
      (t/is-count zero?))

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (authorize "root" "admin_password")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str c/service-context uri)]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (request abs-uri)
        (t/is-status 200)
        (t/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (request base-uri)
                      (t/is-resource-uri collection-uri)
                      (t/is-count pos?)
                      (t/entries :serviceMessages))]
      (is ((set (map :id entries)) uri)))

    ;; update entry with new title
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :put
                 :body (json/write-str {:title "new title"}))
        (t/is-status 200))

    ;; check that update was done
    (-> (session (ring-app))
        (request abs-uri)
        (t/is-status 200)
        (t/is-key-value :title "new title")
        (t/is-key-value :message "description"))

    ;; delete the entry
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :delete)
        (t/is-status 204))

    ;; ensure that it really is gone
    (-> (session (ring-app))
        (request abs-uri)
        (t/is-status 404))))

(deftest bad-methods
  (let [resource-uri (str base-uri "/" (u/random-uuid))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (do
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (t/is-status 405)))))))
