(ns eu.stratuslab.cimi.resources.job-test
  (:require
    [eu.stratuslab.cimi.resources.job :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]
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

