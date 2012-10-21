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
    (let [initial-data (initialize ks)]
      (is (:id initial-data))
      (is (:name initial-data))
      (is (:description initial-data))
      (is (:created initial-data))
      (is (:updated initial-data))
      (let [retrieved (retrieve ks baseURI)
            compare (dissoc retrieved :baseURI :resourceURI)]
          (is (= baseURI (:baseURI retrieved)))
          (is (= resource-uri (:resourceURI retrieved)))
          (is (= initial-data compare)))
        (let [updated (assoc initial-data :property-x "othervalue")]
          (update ks updated)
          (let [retrieved (retrieve ks baseURI)
                compare (dissoc retrieved :updated :baseURI :resourceURI)]
            (is (:updated retrieved))
            (is (not= (:updated initial-data) (:updated retrieved)))
            (is (= baseURI (:baseURI retrieved)))
            (is (= resource-uri (:resourceURI retrieved)))
            (is (= (dissoc updated :updated) compare)))))))
