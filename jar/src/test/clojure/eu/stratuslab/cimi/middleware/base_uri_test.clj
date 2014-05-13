;
; Copyright 2014 Centre National de la Recherche Scientifique (CNRS)
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns eu.stratuslab.cimi.middleware.base-uri-test
  (:require
    [eu.stratuslab.cimi.middleware.base-uri :refer :all]
    [clojure.test :refer [deftest is are]]))

(deftest check-wrapping-works
  (let [context "cimi"
        correct-value "http://example.com:9999/cimi/"
        tfunc (fn [req]
                (is (= correct-value (:base-uri req))))]
    ((wrap-base-uri tfunc context) {:scheme      "http"
                                    :server-name "example.com"
                                    :server-port 9999})))

(deftest check-wrapping-works-without-context
  (let [correct-value "http://example.com:9999/"
        tfunc (fn [req]
                (is (= correct-value (:base-uri req))))]
    ((wrap-base-uri tfunc) {:scheme      "http"
                            :server-name "example.com"
                            :server-port 9999})))
