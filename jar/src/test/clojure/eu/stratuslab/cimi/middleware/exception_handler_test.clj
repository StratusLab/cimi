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

(ns eu.stratuslab.cimi.middleware.exception-handler-test
  (:require
    [eu.stratuslab.cimi.middleware.exception-handler :refer :all]
    [expectations :refer :all]
    [ring.util.response :as r]))

(let [handler (wrap-exceptions (fn [request] (r/response "OK")))
      response (handler {})]
  (expect r/response? response)
  (expect 200 (:status response))
  (expect "OK" (:body response)))

(let [handler (wrap-exceptions (fn [request] (throw (Exception. "UNKNOWN"))))
      response (handler {})]
  (expect r/response? response)
  (expect 500 (:status response)))

(let [handler (wrap-exceptions (fn [request] (throw (ex-info "ERROR" (r/not-found "OK")))))
      response (handler {})]
  (expect r/response? response)
  (expect 404 (:status response))
  (expect "OK" (:body response)))


(run-tests [*ns*])
