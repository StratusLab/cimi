(ns eu.stratuslab.cimi.middleware.cimi-version-header-test
  (:require [eu.stratuslab.cimi.middleware.cimi-version-header :refer :all]
            [clojure.test :refer [deftest is are]]
            [clojure.string :as str]))

(deftest check-spec-version-map
  (doall
   (for [v spec-versions]
     (let [value (get spec-versions-map v)
           correct (str/join "." v)]
       (is (= correct value))))))

(deftest check-version-match?
  (are [r v1 v2] (is (= r (version-match? v1 v2)))
       true  ["1" "0" "0"] ["1" "0" "0"]
       true  ["1" "0"]     ["1" "0" "1"]
       true  ["1" "0"]     ["1" "0" "0"]
       true  ["1"]         ["1" "1" "0"]
       true  ["1"]         ["1" "0" "0"]
       false ["1" "0" "1"] ["1" "0" "0"]
       false ["1" "1"]     ["1" "0" "0"]
       false ["2"]         ["1" "0" "0"]
       true  nil           ["1" "0" "0"]))

(deftest check-parse-version
  (are [v correct] (is (= correct (parse-version v)))
       "1.0.0" ["1" "0" "0"]
       " 1.0.0" ["1" "0" "0"]
       "1.0.0 " ["1" "0" "0"]
       "1.0" ["1" "0"]
       " 1.0" ["1" "0"]
       "1.0 " ["1" "0"]
       "1.0.0." nil
       "1.0." nil
       "1." nil
       "1" nil
       nil nil))

(deftest check-requested-versions-parsing
  (are [s v] (is (= v (requested-versions s)))
       "" []
       nil nil
       "1.0.0" [["1" "0" "0"]]
       " 1.0.0" [["1" "0" "0"]]
       "1.0.0 " [["1" "0" "0"]]
       "1.0" [["1" "0"]]
       "1.0.0,2.3.4" [["1" "0" "0"] ["2" "3" "4"]]
       "1.0.0 , 2.3.4" [["1" "0" "0"] ["2" "3" "4"]]
       "1.0.0 , 2.3" [["1" "0" "0"] ["2" "3"]]
       "1.0.0 , 2..3" [["1" "0" "0"]]
       "1 , 2.3.4" [["2" "3" "4"]]))

(deftest check-selected-versions
  (are [input correct] (is (= correct (select-spec-version input)))
       "1.0.0" "1.0.0"
       "1.0" "1.0.0"
       "2.3.4,1.0.0" "1.0.0"
       "2.3.4,1.0" "1.0.0"
       "2.,1.0.0" "1.0.0"
       "2.,1.0" "1.0.0"
       "1.0.0." nil
       "1.0.1" nil
       "1.1" nil
       "" nil
       nil "1.0.0"))

(deftest check-add-cimi-spec-key
  (is (= "1.0.0" (:cimi-specification-version (add-cimi-spec-key {} "1.0.0")))))

(deftest check-add-cimi-spec-header
  (is (= "1.0.0" (get-in (add-cimi-spec-header {} "1.0.0") [:headers cimi-header]))))

(defn get-dummy-handler
  "Returns a handler that checks that the :cimi-specification-version
  key exists in the request and that it has the given version."
  [version]
  (fn [req]
    (let [v (:cimi-specification-version req)]
      (is (= v version)))
    {}))

(deftest check-wrap-cimi-version-header
  (are [input correct] (let [handler (wrap-cimi-version-header (get-dummy-handler correct))
                             req {:headers {cimi-header input}}
                             resp (handler req)
                             result (get-in resp [:headers cimi-header])]
                         (is (= correct result)))
       "1.0.0" "1.0.0"
       "1.0" "1.0.0"
       "2.3.4,1.0.0" "1.0.0"
       "2.3.4,1.0" "1.0.0"
       "2.,1.0.0" "1.0.0"
       "2.,1.0" "1.0.0"
       nil "1.0.0"))

(deftest check-wrap-cimi-version-header-with-missing-header
  (let [correct "1.0.0"
        handler (wrap-cimi-version-header (get-dummy-handler correct))
        req {}
        resp (handler req)
        result (get-in resp [:headers cimi-header])]
    (is (= correct result))))

(deftest check-wrap-cimi-version-header-raises-error
  (are [input] (let [handler (wrap-cimi-version-header (get-dummy-handler "1.0.0"))
                     req {:headers {cimi-header input}}
                     resp (handler req)
                     status (:status resp)]
                 (is (= 400 status)))
       "1.0.0."
       "1.0.1"
       "1.1"
       ""
       " , "))
