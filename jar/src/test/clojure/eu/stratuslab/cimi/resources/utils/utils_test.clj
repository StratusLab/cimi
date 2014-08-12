(ns eu.stratuslab.cimi.resources.utils.utils-test
  (:require [eu.stratuslab.cimi.resources.utils.utils :refer :all]
            [eu.stratuslab.cimi.couchbase-test-utils :as t]
            [clojure.test :refer :all]
            [couchbase-clj.client :as cbc])
  (:import [java.util UUID]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(deftest check-uuid
  (let [uuid (random-uuid)]
    (is (string? uuid))
    (is (UUID/fromString uuid))))

(deftest test-strip-common-attrs
  (let [entry {:id "/DummyResource/10"
               :name "name"
               :description "description"
               :created "1964-08-25T10:00:00.0Z"
               :updated "1964-08-25T10:00:00.0Z"
               :resourceURI "http://example.org/DummyResource"
               :properties {"a" 1 "b" 2}}
        correct {:resourceURI "http://example.org/DummyResource"}]
    (is (= correct (strip-common-attrs entry)))))

(deftest test-strip-service-attrs
  (let [entry {:id "/DummyResource/10"
               :name "name"
               :description "description"
               :created "1964-08-25T10:00:00.0Z"
               :updated "1964-08-25T10:00:00.0Z"
               :resourceURI "http://example.org/DummyResource"
               :properties {"a" 1 "b" 2}
               :operations [{:rel "add" :href "/add"}]}
        correct {:name "name"
                 :description "description"
                 :properties {"a" 1 "b" 2}}]
    (is (= correct (strip-service-attrs entry)))))

(deftest check-set-time-attributes
  (let [m (update-timestamps {})]
    (is (:created m))
    (is (:updated m)))
  (let [m (update-timestamps {:created "dummy"})]
    (is (= "dummy" (:created m)))
    (is (:updated m))))

(deftest check-correct-resource?
  (let [resourceURI "http://example.org/DummyResource"
        resource {:resourceURI resourceURI}]
    (is (correct-resource? resourceURI resource))
    (is (not (correct-resource? "http://example.org/BAD" resource)))))

;;
;; Following tests require access to Couchbase
;;

(deftest check-get-resource
  (let [uri "DummyResource/10"
        resource {:name "name" :description "check-get-resource"}]
    (cbc/add-json t/*test-cb-client* uri resource)
    (is (= resource (get-resource t/*test-cb-client* uri)))
    (is (thrown? Exception (get-resource t/*test-cb-client* "BAD URI")))))

(deftest check-resolve-href-identities
  (are [x] (= x (resolve-href x))
           1
           "a"
           [1 2 3]
           {}
           {:name "name"}
           {:name "name" :properties {"a" 1 "b" 2} :data [1 2 3 4 5]}))

(deftest check-resolve-href
  (let [data1 {:name "BAD" :alpha "A" :beta "B"}]
    (cbc/add-json t/*test-cb-client* "Data/1" data1)

    (are [x correct] (= correct (resolve-href x))
                     {:href "Data/1"} {:alpha "A" :beta "B"}
                     {:href "Data/1" :name "BAD"} {:alpha "A" :beta "B"}
                     {:href "Data/1" :alpha "OK"} {:alpha "OK" :beta "B"}
                     {:href "Data/1" :alpha "OK" :beta "OK"} {:alpha "OK" :beta "OK"}
                     ))
  (is (thrown? Exception (resolve-href {:href "Data/BAD"}))))

(deftest check-resolve-hrefs
  (let [data1 {:name "BAD" :value "BAD" :other "OK"}
        data2 {:four {:href "Data/3"} :two 2 :value "BAD"}
        data3 {:three 3 :name "BAD"}
        data4 {:name "4"
               :alpha {:href "Data/1" :value "OK"}
               :beta {:href "Data/2" :value "OK" :name "BAD"}}
        correct {:name "4"
                 :alpha {:value "OK" :other "OK"}
                 :beta {:two 2 :four {:three 3} :value "OK"}}]
    (cbc/add-json t/*test-cb-client* "Data/1" data1)
    (cbc/add-json t/*test-cb-client* "Data/2" data2)
    (cbc/add-json t/*test-cb-client* "Data/3" data3)
    (cbc/add-json t/*test-cb-client* "Data/4" data4)
    (is (= correct (resolve-hrefs t/*test-cb-client* data4)))))
