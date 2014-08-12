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

(ns eu.stratuslab.cimi.cb.utils
  (:require
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]))

(defn not-degraded?
  "Returns true if the given server statistics indicate that the server
   is not in degraded mode."
  [m]
  (= "0" (get m "ep_degraded_mode" "1")))

(defn ready?
  "Verifies that all servers are ready (i.e. not in a degraded state)."
  [cb-client]
  (->> (cbc/get-client-status cb-client)
       (vals)
       (every? not-degraded?)))

(defn wait-until-ready
  "The internal Couchbase initialization can take a long time.  This function
   will wait until all of the servers associated with the client are ready.
   The function will timeout after 12s and write a failure into the log."
  [cb-client]
  (let [sleep-time 2000
        timeout 12000
        t0 (System/currentTimeMillis)]
    (loop []
      (if (ready? cb-client)
        true
        (let [_ (Thread/sleep sleep-time)
              delta (- (System/currentTimeMillis) t0)
              expired? (> delta timeout)]
          (if expired?
            (do
              (log/error "timeout while waiting for Couchbase server(s) to become ready")
              false)
            (recur)))))))

