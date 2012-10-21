(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
            [eu.stratuslab.cimi.cassandra-test-utils :refer :all]
            [eu.stratuslab.cimi.resources.common :as common]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [clj-hector.core :refer [keyspace]])
  (:import [java.util UUID]
           [clojure.lang ExceptionInfo]))

(def ^:const baseURI "https://localhost:cimi")

(use-fixtures :each start-daemon-fixture)

(deftest check-cloud-entry-point-lifecycle
  (with-test-keyspace-opts ks resource-name common/common-resource-attrs
    (let [data {:id resource-name
                :name "Test StratusLab Deployment"
                :description "Test StratusLab Deployment Desc."
                :property-x "value"
                :property-y "value"}
          row-id (update ks data)]
      (is row-id)
      (let [retrieved (retrieve ks baseURI)
            compare (-> retrieved
                        (dissoc :created :updated :baseURI :resourceURI))]
        (is (:created retrieved))
        (is (:updated retrieved))
        (is (= baseURI (:baseURI retrieved)))
        (is (= resource-uri (:resourceURI retrieved)))
        (is (= data compare)))
      (let [updated (assoc data :property-x "othervalue")]
        (update ks updated)
        (let [retrieved (retrieve ks baseURI)
              compare (-> retrieved
                          (dissoc :created :updated :baseURI :resourceURI))]
          (is (:created retrieved))
          (is (:updated retrieved))
          (is (= baseURI (:baseURI retrieved)))
          (is (= resource-uri (:resourceURI retrieved)))
          (is (= updated compare)))))))
