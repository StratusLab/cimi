(ns eu.stratuslab.cimi.resources.volume-image-test
  (:require
    [eu.stratuslab.cimi.resources.volume-image :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [eu.stratuslab.cimi.routes :as routes]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-entry
  {:acl {:owner {:principal "::ADMIN" :type "ROLE"}}
   :state "CREATING"
   :imageLocation {:href "GWE_nifKGCcXiFk42XaLrS8LQ-J"}
   :bootable true})

(deftest test-image-id-check
  (let [id "GWE_nifKGCcXiFk42XaLrS8LQ-J"]
    (is (= id (image-id {:imageLocation {:href id}})))
    (is (nil? (image-id {:imageLocation {:href "BAD"}})))
    (is (nil? (image-id {})))))

(deftest lifecycle

  ;; anonymous create fails
  (-> (session (ring-app))
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; anonymous query fails
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

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
                      (t/entries :volumeImages))]
      (is ((set (map :id entries)) uri)))

    ;; delete the entry
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri
                 :request-method :delete)
        (t/is-status 202)
        (t/has-job))))

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
