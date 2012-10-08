(ns eu.stratuslab.cimi.utils.filter-test
  (:require [eu.stratuslab.cimi.utils.filter :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer [deftest is are]]
            [com.lithinos.amotoen.core :refer [pegasus post-process validate wrap-string]]))

(defn process
  [rule f s]
  (post-process rule filter-grammar (wrap-string s) {rule f}))

(deftest check-grammar-validity
  (is (validate filter-grammar)))

(deftest check-names
  (are [input correct] (let [result (process :Name value-as-string input)
                             _ (println result)]
                         (is (= correct result)))
       "a" "a"
       "ab" "ab"
       "abc" "abc"
       "_" "_"
       "_a" "_a"
       "_ab" "_ab"
       "a1" "a1"
       "a12" "a12"
       "1" "" ; should be an error?
       "12" "" ; should be an error?
       ))

(deftest check-integers
  (are [input correct] (let [result (process :IntValue value-as-string input)
                             _ (println result)]
                         (is (= correct result)))
       "0" "0"
       "10" "10"
       "101" "101"
       "a" "" ; should be an error?
       "ab" "" ; should be an error?
       ))

(deftest check-booleans
  (are [input correct] (let [result (process :BoolValue identity input)
                             _ (println result)]
                         (is (= correct result)))
       "true" "true"
       "false" "false"
       "x" nil ; should be an error?
       ))

(deftest check-single-quoted-values
  (are [input correct] (let [result (process :single-quoted-value process-string-value input)
                             _ (println result)]
                         (is (= correct result)))
       "" ""
       "alpha" "alpha"
       "alpha\\'beta" "alpha\\'beta"
       ))

(deftest check-double-quoted-values
  (are [input correct] (let [result (process :double-quoted-value process-string-value input)
                             _ (println result)]
                         (is (= correct result)))
       "" ""
       "alpha" "alpha"
       "alpha\\\"beta" "alpha\\\"beta"
       ))

(comment
(deftest try-filter-grammar3
  (let [result (pegasus :Filter filter-grammar (wrap-string "beta=4"))]
    (pprint result)))

(deftest try-filter-grammar4
  (let [result (pegasus :Filter filter-grammar (wrap-string "property['x']=444"))]
    (pprint result)))

(deftest check-booleans
  (are [input correct] (let [m (pegasus :BoolValue filter-grammar (wrap-string input))]
                         (is (= correct m)))))
)
