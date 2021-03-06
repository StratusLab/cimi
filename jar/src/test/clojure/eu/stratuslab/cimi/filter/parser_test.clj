(ns eu.stratuslab.cimi.filter.parser-test
  (:require
    [eu.stratuslab.cimi.filter.parser :refer :all]
    [instaparse.core :as insta]
    [clojure.test :refer [deftest are]]))

(deftest check-valid-double-quote-strings
  (let [parse (insta/parser grammar-url :start :DoubleQuoteString)]
    (are [s] (not (insta/failure? (parse s)))
             "\"\""
             "\"a\""
             "\"a1\""
             "\"\\\"a\""
             "\"a\\\"\""
             "\"a\\\"a\"")))

(deftest check-valid-single-quote-strings
  (let [parse (insta/parser grammar-url :start :SingleQuoteString)]
    (are [s] (not (insta/failure? (parse s)))
             "''"
             "'a'"
             "'a1'"
             "'\\'a'"
             "'a\\''"
             "'a\\'a'")))

(deftest check-valid-dates
  (let [parse (insta/parser grammar-url :start :DateValue)]
    (are [date] (not (insta/failure? (parse date)))
                "2012-01-02"
                "2012-01-02T13:14:25Z"
                "2012-01-02T13:14:25.6Z"
                "2012-01-02T13:14:25-01:15"
                "2012-01-02T13:14:25.6-01:15"
                "2012-01-02T13:14:25+02:30"
                "2012-01-02T13:14:25.6+02:30")))

(deftest check-invalid-dates
  (let [parse (insta/parser grammar-url :start :DateValue)]
    (are [date] (insta/failure? (parse date))
                "2012"
                "2012-01-99T13:14:25.6ZZ"
                "2012-01-02T13:14:25.6Q"
                "2012-01:02T25:14:25.6-01:15"
                "2012-01-02T13:14:25.6+02-30")))

(deftest check-valid-filters
  (let [parse (insta/parser grammar-url)]
    (are [filter] (not (insta/failure? (parse filter)))
                  "alpha=3"
                  "3=alpha"
                  "alpha=3 and beta=4"
                  "3=alpha and 4=beta"
                  "(alpha=3)"
                  "(3=alpha)"
                  "property['beta']='4'"
                  "property['beta']!='4'"
                  "property['beta']<'4'" ;; strictly this should be illegal
                  "alpha=3 and beta=4"
                  "alpha=3 or beta=4"
                  "alpha=3 and beta=4 or gamma=5 and delta=6"
                  "alpha=3 and (beta=4 or gamma=5) and delta=6")))

(deftest check-invalid-filters
  (let [parse (insta/parser grammar-url)]
    (are [filter] (insta/failure? (parse filter))
                  ""
                  "()"
                  "alpha=beta"
                  "alpha=3.2"
                  "alpha&&4"
                  "property[beta]='4'"
                  "property['beta']=4"
                  "4=property['beta']")))
