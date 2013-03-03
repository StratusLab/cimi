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
