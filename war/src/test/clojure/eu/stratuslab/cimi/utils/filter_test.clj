(ns eu.stratuslab.cimi.utils.filter-test
  (:require [eu.stratuslab.cimi.utils.filter :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer [deftest is are]]
            [com.lithinos.amotoen.core :refer [pegasus validate wrap-string]]))

(deftest check-grammar-validity
  (is (validate filter-grammar)))

(deftest try-filter-grammar2
  (let [result (pegasus :Filter filter-grammar (wrap-string "beta=true"))]
    (pprint result)))

(deftest try-filter-grammar3
  (let [result (pegasus :Filter filter-grammar (wrap-string "beta=4"))]
    (pprint result)))

(deftest try-filter-grammar4
  (let [result (pegasus :Filter filter-grammar (wrap-string "property['x']=444"))]
    (pprint result)))

