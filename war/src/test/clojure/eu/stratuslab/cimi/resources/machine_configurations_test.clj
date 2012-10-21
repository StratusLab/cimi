(ns eu.stratuslab.cimi.resources.machine-configurations-test
  (:require [eu.stratuslab.cimi.resources.machine-configurations :refer :all]
            [eu.stratuslab.cimi.cassandra-test-utils :refer :all]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clj-hector.core :refer [keyspace]])
  (:import [java.util UUID]
           [clojure.lang ExceptionInfo]))

(use-fixtures :each start-daemon-fixture)

(deftest check-machine-configuration-lifecycle
  (with-test-keyspace-opts ks resource-name resource-attrs
    (let [data {:id "c1.xlarge"
                :description "myconfig"
                :cpu 10
                :property-x "value"
                :property-y "value"
                :cpuArch "x86_64"}
          row-id (create ks data)]
      (is row-id)
      (let [retrieved (retrieve ks row-id)
            compare (dissoc retrieved :created :updated)]
        (is (:created retrieved))
        (is (:updated retrieved))
        (is (= data compare)))
      (let [updated (assoc data :property-x "othervalue")]
        (update ks row-id updated)
        (let [retrieved (retrieve ks row-id)
              compare (dissoc retrieved :created :updated)]
          (is (:created retrieved))
          (is (:updated retrieved))
          (is (= updated compare))))
      (delete ks row-id)
      (let [retrieved (retrieve ks row-id)]
        (is (= {} retrieved))))))
