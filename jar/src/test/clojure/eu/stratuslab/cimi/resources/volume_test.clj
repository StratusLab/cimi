(ns eu.stratuslab.cimi.resources.volume-test
  (:require
    [eu.stratuslab.cimi.resources.volume :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [eu.stratuslab.cimi.routes :as routes]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-entry
  {:acl {:owner {:principal "::ADMIN" :type "ROLE"}}
   :state "CREATING"
   :type "http://schemas.cimi.stratuslab.eu/normal"
   :capacity 1024
   :bootable true
   :eventLog "EventLog/uuid"})

(def valid-template
  {:resourceURI "http://schemas.dmtf.org/cimi/1/VolumeCreate"
   :name "template"
   :description "dummy template"
   :volumeTemplate {:volumeConfig {:type "http://schemas.cimi.stratuslab.eu/normal"
                                   :format "ext4"
                                   :capacity 1024}
                    :volumeImage {:state "AVAILABLE"
                                  :imageLocation {:href "https://marketplace.stratuslab.eu/A"}
                                  :bootable true}}})

(deftest lifecycle

  ;; anonymous create should fail
  (-> (session (ring-app))
      (request base-uri
               :request-method :post
               :body (json/write-str valid-template))
      (t/is-status 403))

  ;; anonymous query should also fail
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

  ;; create a volume from a template
  (let [uri (-> (session (ring-app))
                (authorize "jane" "user_password")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-template))
                (t/is-status 201)
                (t/has-job)
                (t/location))
        abs-uri (str "/" uri)]

    (is uri)

    ;; check that volume was created
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri)
        (t/is-status 200)
        (t/is-key-value :name "template")
        (t/is-key-value :description "dummy template"))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (authorize "jane" "user_password")
                      (request base-uri)
                      (t/is-status 200)
                      (t/is-resource-uri collection-uri)
                      (t/is-count pos?)
                      (t/entries :volumes))]
      (is ((set (map :id entries)) uri)))

    ;; delete the entry -- asynchronous
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
