(ns eu.stratuslab.cimi.resources.volume-test
  (:require
    [eu.stratuslab.cimi.resources.volume :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.validation :refer [validation-errors]]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:state "CREATING"
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

  ;; create a volume from a template
  (let [uri (-> (session (ring-app))
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
       (request abs-uri)
       (t/is-status 200)
       (t/is-key-value :name "template")
       (t/is-key-value :description "dummy template"))

   ;; query to see that entry is listed
   (let [entries (-> (session (ring-app))
                     (request base-uri)
                     (t/is-resource-uri collection-type-uri)
                     (t/is-count pos?)
                     (t/entries :volumes))]
     (is ((set (map :id entries)) uri)))

   ;; delete the entry -- asynchronous
   (-> (session (ring-app))
       (request abs-uri
                :request-method :delete)
       (t/is-status 202)
       (t/has-job))))
