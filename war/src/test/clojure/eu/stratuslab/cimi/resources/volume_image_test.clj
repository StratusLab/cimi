(ns eu.stratuslab.cimi.resources.volume-image-test
  (:require
   [eu.stratuslab.cimi.resources.volume-image :refer :all]
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
  {:state "CREATING"
   :imageLocation {:href "GWE_nifKGCcXiFk42XaLrS8LQ-J"}
   :bootable true} )

(deftest test-image-id-check 
  (let [id "GWE_nifKGCcXiFk42XaLrS8LQ-J"]
    (is (= id (image-id {:imageLocation {:href id}})))
    (is (nil? (image-id {:imageLocation {:href "BAD"}})))
    (is (nil? (image-id {})))))

(deftest test-volume-image-schema
  (let [volume-image (assoc valid-entry
             :id "VolumeImage/10"
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]
        (is (empty? (validation-errors VolumeImage volume-image)))
        (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :state)))))
        (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :imageLocation)))))
        (is (not (empty? (validation-errors VolumeImage (assoc volume-image :imageLocation {})))))
        (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :bootable)))))))

