(ns eu.stratuslab.cimi.resources.volume-template-test
  (:require
    [eu.stratuslab.cimi.resources.volume-template :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app routes))

(def valid-entry
  {:acl {:owner {:principal "::ADMIN" :type "ROLE"}}
   :volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage {:href "VolumeImage/mkplaceid"}})

(deftest lifecycle

  ;; anonymous create will fail
  (-> (session (ring-app))
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; anonymous query will also fail
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

  ;; user query will succeed
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-count zero?))

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (authorize "jane" "user_password")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str "/" uri)]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri)
        (t/is-status 200)
        (t/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (authorize "jane" "user_password")
                      (request base-uri)
                      (t/is-status 200)
                      (t/is-resource-uri collection-uri)
                      (t/is-count pos?)
                      (t/entries :volumeTemplates))]
      (is ((set (map :id entries)) uri)))

    ;; delete the entry
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri
                 :request-method :delete)
        (t/is-status 200))

    ;; ensure that it really is gone
    (-> (session (ring-app))
        (authorize "jane" "user_password")
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
