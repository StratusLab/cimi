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
    [expectations :refer :all]
    [clojure.test :refer [deftest is are]]))

(def request {:headers {"host" "external.example.org:9998"
                        "x-forwarded-proto" "https"}
              :scheme :http
              :server-name "internal.example.com"
              :server-port 9999})

(def wrapper (wrap-base-uri identity))

(expect "external.example.org:9998" (get-host-port request))
(expect "internal.example.com:9999" (get-host-port (dissoc request :headers)))

(expect "https" (get-scheme request))
(expect "http" (get-scheme (dissoc request :headers)))

(expect "https://external.example.org:9998/cimi/" (construct-base-uri request))
(expect "http://internal.example.com:9999/cimi/" (construct-base-uri (dissoc request :headers)))

(expect "https://external.example.org:9998/cimi/" (:base-uri (wrapper request)))
(expect "http://internal.example.com:9999/cimi/" (:base-uri (wrapper (dissoc request :headers))))


(run-tests [*ns*])
