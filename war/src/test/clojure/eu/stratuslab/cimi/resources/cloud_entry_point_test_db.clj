(ns eu.stratuslab.cimi.resources.cloud-entry-point-test-db
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as cb-utils]
    [clojure.test :refer :all]))

(use-fixtures :once cb-utils/temp-bucket-fixture)

