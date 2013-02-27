(ns eu.stratuslab.cimi.filter-parser-test
  (:require
    [eu.stratuslab.cimi.filter-parser :refer [Filter StringValue datetime-lit]]
    [blancas.kern.core :as kern]
    [clojure.test :refer [deftest is are]]
    [clojure.string :as str]))

(deftest check-valid-filters
  (let [valid-filters ["alpha=3",
                       "3=alpha",
                       "alpha=3 and beta=4",
                       "3=alpha and 4=beta",
                       " (alpha=3)", ;; this fails in strange way with no leading space?!
                       " (3=alpha)", ;; ditto!
                       "property['beta']='4'",
                       "alpha=3 and beta=4",
                       "alpha=3 or beta=4",
                       "alpha=3 and beta=4 or gamma=5 and delta=6"]]
    (doall
      (for [filter valid-filters]
        (is (:ok (kern/parse Filter filter)) filter)))))

(deftest check-valid-strings
  (let [valid-strings ["\"\"",
                       "\"a\"",
                       "\"a1\"",
                       "\"\\\"a\"",
                       "\"a\\\"\"",
                       "\"a\\\"a\"",
                       "''",
                       "'a'",
                       "'a1'",
                       "'\\'a'",
                       "'a'\\'",
                       "'a\\'a'"]]
    (doall
      (for [s valid-strings]
        (is (:ok (kern/parse StringValue s)) s)))))

(deftest check-valid-dates
  (let [valid-dates ["2012-01-02",
                     "2012-01-02T13:14:25.6Z",
                     "2012-01-02T13:14:25.6-01:15",
                     "2012-01-02T13:14:25.6+02:30"]]
    (doall
      (for [date valid-dates]
        (is (:ok (kern/parse datetime-lit date)) date)))))

(comment (deftest check-invalid-dates
  (let [invalid-dates ["2012",
                       "2012-01-99T13:14:25.6Z",
                       "2012-01-99T13:14:25.6Q",
                       "2012-01:02T25:14:25.6-01:15",
                       "2012-01-02T13:14:25.6+02-30"]]
    (doall
      (for [date invalid-dates]
        (is (not (:ok (kern/parse datetime-lit date))) date))))))

