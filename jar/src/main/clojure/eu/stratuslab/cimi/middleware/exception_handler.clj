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

(ns eu.stratuslab.cimi.middleware.exception-handler
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as r]
            [clj-stacktrace.repl :refer [pst-str]]))

(defn treat-unexpected-exception
  [e]
  (let [msg (str "Unexpected exception thrown: " (str e))
        st (pst-str e)
        body {:status 500 :message msg}
        response (-> (r/response body)
                     (r/status 500))]
    (log/error (str msg "\n" st))
    response))

(defn wrap-exceptions [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (let [response (ex-data e)]
             (if (r/response? response)
               response
               (treat-unexpected-exception e)))))))
