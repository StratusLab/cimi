(ns eu.stratuslab.cimi.resources.job-test
  (:require
    [eu.stratuslab.cimi.resources.job :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.validation :refer [validation-errors]]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:state "QUEUED"
   :targetResource "Machine/uuid-1"
   :affectedResources ["Machine/uuid-2"]
   :action "http://schemas.cimi.stratuslab.eu/create-volume"
   :returnCode 0
   :progress 0
   :statusMessage "none"
   :timeOfStatusChange "20130825T10:00:00.00Z"
   :parentJob "Job/uuid-1"
   :nestedJobs ["Job/uuid-2"]})

(deftest test-machine-configuration-schema
  (let [job (assoc valid-entry
              :id "/Job/10"
              :resourceURI type-uri
              :created "1964-08-25T10:00:00.0Z"
              :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors Job job)))
    (is (empty? (validation-errors Job (dissoc job :state))))
    (is (empty? (validation-errors Job (dissoc job :affectedResources))))
    (is (empty? (validation-errors Job (dissoc job :returnCode))))
    (is (empty? (validation-errors Job (dissoc job :progress))))
    (is (empty? (validation-errors Job (dissoc job :statusMessage))))
    (is (empty? (validation-errors Job (dissoc job :timeOfStatusChange))))
    (is (empty? (validation-errors Job (dissoc job :parentJob))))
    (is (empty? (validation-errors Job (dissoc job :nestedJobs))))
    (is (not (empty? (validation-errors Job (dissoc job :targetResource)))))
    (is (not (empty? (validation-errors Job (dissoc job :action)))))))

