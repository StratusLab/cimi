(ns eu.stratuslab.cimi.resources.utils-test
  (:require [eu.stratuslab.cimi.resources.utils :refer :all]
            [clojure.test :refer :all])
  (:import [java.util UUID]))

(deftest check-uuid
  (let [uuid (create-uuid)]
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
  (let [m (set-time-attributes {})]
    (is (:created m))
    (is (:updated m)))
  (let [m (set-time-attributes {:created "dummy"})]
    (is (= "dummy" (:created m)))
    (is (:updated m))))

(deftest check-correct-resource?
  (let [resourceURI "http://example.org/DummyResource"
        resource {:resourceURI resourceURI}]
    (is (correct-resource? resourceURI resource))
    (is (not (correct-resource? "http://example.org/BAD" resource)))))


