(ns eu.stratuslab.cimi.views.machine-configurations-test
  (:require [eu.stratuslab.cimi.views.machine-configurations :refer :all]
            [eu.stratuslab.cimi.cassandra-test-utils :refer :all]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clj-hector.core :refer [keyspace]])
  (:import [java.util UUID]
           [clojure.lang ExceptionInfo]))

(use-fixtures :each start-daemon-fixture)

(deftest check-create-machine-configuration
  (with-test-keyspace-opts ks cf-name column-metadata
    (let [data {:id "c1.xlarge" :description "myconfig" :cpu 10 :property-x "value" :property-y "value"}
          row-id (create ks data)]
      (println row-id)
      (pprint (retrieve ks row-id)))))



