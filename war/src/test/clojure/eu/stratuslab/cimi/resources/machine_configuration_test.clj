(ns eu.stratuslab.cimi.resources.machine-configuration-test
  (:require
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [eu.stratuslab.cimi.resources.machine-configuration :refer :all]
   [clojure.test :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)
