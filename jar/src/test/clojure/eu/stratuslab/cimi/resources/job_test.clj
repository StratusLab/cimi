(ns eu.stratuslab.cimi.resources.job-test
  (:require
    [eu.stratuslab.cimi.resources.job :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app routes))

(def valid-entry
  {:acl {:owner {:principal "::ADMIN" :type "ROLE"}}
   :state "QUEUED"
   :targetResource "Machine/uuid-1"
   :affectedResources ["Machine/uuid-2"]
   :action "http://schemas.cimi.stratuslab.eu/create-volume"
   :returnCode 0
   :progress 0
   :statusMessage "none"
   :timeOfStatusChange "20130825T10:00:00.00Z"
   :parentJob "Job/uuid-1"
   :nestedJobs ["Job/uuid-2"]})

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

  ;; anonymous query should fail
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

  ;; user query should be ok
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri collection-type-uri)
      (t/is-count zero?))

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (authorize "root" "admin_password")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str "/" uri)]

    (is uri)

    ;; verify that the new entry is accessible
    ;; timeOfStatusChange is taken out of comparison because it is set by service
    (-> (session (ring-app))
        (request abs-uri)
        (t/is-status 200)
        (t/does-body-contain (dissoc valid-entry :timeOfStatusChange)))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (authorize "root" "admin_password")
                      (request base-uri)
                      (t/is-resource-uri collection-type-uri)
                      (t/is-count pos?)
                      (t/entries :jobs))]
      (is ((set (map :id entries)) uri)))

    ;; update entry with new title
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :put
                 :body (json/write-str {:state "RUNNING"}))
        (t/is-status 200))

    ;; check that update was done
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri)
        (t/is-status 200)
        (t/is-key-value :state "RUNNING")
        (t/is-key-value :targetResource "Machine/uuid-1"))

    ;; delete the entry
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :delete)
        (t/is-status 200))

    ;; ensure that it really is gone
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri)
        (t/is-status 404))))

(deftest bad-methods
  (let [resource-uri (str base-uri "/" (u/create-uuid))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]]]
        (do
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (t/is-status 405)))))))
