(ns eu.stratuslab.cimi.middleware.cb-client-test
  (:require
    [eu.stratuslab.cimi.middleware.cb-client :refer :all]
    [clojure.test :refer [deftest is]]))

(deftest check-wrapping-works
  (let [correct-value "OK"
        tfunc (fn [req]
                (is (= correct-value (:cb-client req))))]
    ((wrap-cb-client correct-value tfunc) {})))
