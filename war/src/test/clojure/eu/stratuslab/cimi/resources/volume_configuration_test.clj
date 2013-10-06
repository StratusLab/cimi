(ns eu.stratuslab.cimi.resources.volume-configuration-test
  (:require
   [eu.stratuslab.cimi.resources.volume-configuration :refer :all]
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
  {:type "http://stratuslab.eu/cimi/1/raw"
   :format "ext4"
   :capacity 1000} )

(deftest test-volume-configuration-schema
  (let [uri (uuid->uri (utils/create-uuid))
        volume-configuration (assoc valid-entry
             :id uri
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]
        (is (empty? (validation-errors VolumeConfiguration volume-configuration)))
        (is (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :type))))
        (is (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :format))))
        (is (not (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :capacity)))))))

