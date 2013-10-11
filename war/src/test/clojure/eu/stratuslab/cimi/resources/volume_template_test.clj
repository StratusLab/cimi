(ns eu.stratuslab.cimi.resources.volume-template-test
  (:require
    [eu.stratuslab.cimi.resources.volume-template :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.validation :refer [validation-errors]]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage {:href "VolumeImage/mkplaceid"}})

(deftest lifecycle

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str "/" uri)]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (request abs-uri)
        (t/is-status 200)
        (t/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (request base-uri)
                      (t/is-resource-uri collection-type-uri)
                      (t/is-count pos?)
                      (t/entries :volumeTemplates))]
      (is ((set (map :id entries)) uri)))

    ;; delete the entry
    (-> (session (ring-app))
        (request abs-uri
                 :request-method :delete)
        (t/is-status 200))

    ;; ensure that it really is gone
    (-> (session (ring-app))
        (request abs-uri)
        (t/is-status 404))))
