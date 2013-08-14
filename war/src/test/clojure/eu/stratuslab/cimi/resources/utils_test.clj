(ns eu.stratuslab.cimi.resources.utils-test
  (:require [eu.stratuslab.cimi.resources.utils :refer :all]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]))

(deftest check-set-time-attributes
  (let [m (set-time-attributes {})]
    (is (:created m))
    (is (:updated m)))
  (let [m (set-time-attributes {:created "dummy"})]
    (is (= "dummy" (:created m)))
    (is (:updated m))))

(deftest test-strip-service-attrs
  (let [entry {:id "/DummyResource/10"
               :created "1964-08-25T10:00:00.0Z"
               :updated "1964-08-25T10:00:00.0Z"
               :resourceURI "http://example.org/DummyResource"
               :operations [{:rel "add" :href "/add"}]
               :name "name"}]
    (is (= {:name "name"} (strip-service-attrs entry)))))
