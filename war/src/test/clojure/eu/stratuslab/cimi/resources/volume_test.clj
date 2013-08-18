(ns eu.stratuslab.cimi.resources.volume-test
  (:require
   [eu.stratuslab.cimi.resources.volume :refer :all]
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
   :type "http://schemas.cimi.stratuslab.eu/normal"
   :capacity 1024
   :bootable true
   :eventLog "EventLog/uuid"} )

(deftest test-machine-configuration-schema
  (let [volume (assoc valid-entry
             :id "Volume/10"
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]
        (is (empty? (validation-errors Volume volume)))
        (is (empty? (validation-errors Volume (dissoc volume :state))))
        (is (empty? (validation-errors Volume (dissoc volume :bootable))))
        (is (empty? (validation-errors Volume (dissoc volume :eventLog))))
        (is (not (empty? (validation-errors Volume (dissoc volume :type)))))
        (is (not (empty? (validation-errors Volume (dissoc volume :capacity)))))))

