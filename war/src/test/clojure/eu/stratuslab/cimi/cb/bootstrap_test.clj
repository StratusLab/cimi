(ns eu.stratuslab.cimi.cb.bootstrap-test
  (:require
   [eu.stratuslab.cimi.cb.bootstrap :refer :all]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [clojure.test :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(deftest apply-bootstrap-to-empty-db
  (bootstrap t/*test-cb-client*))
