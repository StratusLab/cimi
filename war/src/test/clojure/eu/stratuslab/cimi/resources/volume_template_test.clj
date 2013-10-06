(ns eu.stratuslab.cimi.resources.volume-template-test
  (:require
   [eu.stratuslab.cimi.resources.volume-template :refer :all]
   [eu.stratuslab.cimi.resources.utils :as utils]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [clj-schema.validation :refer [validation-errors]]
   [clojure.test :refer :all]
   [clojure.data.json :as json]
   [peridot.core :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage {:href "VolumeImage/mkplaceid"}})

(deftest test-volume-template-schema
  (let [uri (uuid->uri (utils/create-uuid))
        volume-template (assoc valid-entry
             :id uri
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]
        (is (empty? (validation-errors VolumeTemplate volume-template)))
        (is (empty? (validation-errors VolumeTemplate (dissoc volume-template :volumeConfig))))
        (is (empty? (validation-errors VolumeTemplate (dissoc volume-template :volumeImage))))))

