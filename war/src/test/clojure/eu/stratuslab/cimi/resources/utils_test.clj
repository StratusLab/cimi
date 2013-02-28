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
    (is (:updated m)))
  (let [m (set-time-attributes true {})]
    (is (nil? (:created m)))
    (is (:updated m)))
  (let [m (set-time-attributes nil {})]
    (is (:created m))
    (is (:updated m))
    (is (= (:created m) (:updated m)))))

(deftest check-set-db-id
  (let [m (set-db-id {} "dummy")]
    (is (= "dummy" (:_id m)))))

(deftest check-property-key
  (are [input correct] (is (= correct (property-key input)))
       "properties-" nil
       "alpha" nil
       "properties-x" "x"
       "properties-abc" "abc"))

(deftest check-nest-flatten-properties
  (let [flat {:a 1 :b 2 :properties-a 3 :properties-b 4}
        nested {:a 1 :b 2 :properties {"a" 3 "b" 4}}]
    (is (= flat (flatten-properties nested)))
    (is (= nested (nest-properties flat)))))
