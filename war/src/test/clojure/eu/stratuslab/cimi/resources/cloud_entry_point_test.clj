(ns eu.stratuslab.cimi.resources.cloud-entry-point-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [clojure.test :refer [deftest is are]])
  (:import
    [java.util UUID]
    [clojure.lang ExceptionInfo]))

(def ^:const baseURI "https://localhost:cimi")

(deftest check-strip-unknown-attributes
  (let [input {:a 1 :b 2 :id "ok"}
        correct {:id "ok"}]
    (is (= correct (strip-unknown-attributes input)))))

(deftest check-strip-immutable-attributes
  (let [input {:a 1 :id "ok" :baseURI "ok"}
        correct {:a 1}]
    (is (= correct (strip-immutable-attributes input)))))
