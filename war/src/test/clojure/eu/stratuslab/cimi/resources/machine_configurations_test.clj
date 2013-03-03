(ns eu.stratuslab.cimi.resources.machine-configurations-test
  (:require
    [eu.stratuslab.cimi.resources.machine-configurations :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as cb-utils]
    [clojure.test :refer :all]))

(use-fixtures :each cb-utils/temp-bucket-fixture)
