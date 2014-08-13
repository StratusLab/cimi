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

(ns eu.stratuslab.cimi.db.cb.utils
  (:require
    [eu.stratuslab.cimi.couchbase-cfg :as cfg]
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc])
  (:import
    [java.net URI]))

(def cb-client-defaults {:uris     [(URI/create "http://localhost:8091/pools")]
                         :bucket   "default"
                         :username ""
                         :password ""})

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

(defn create-cb-client
  "Creates a Couchbase client instance from the given configuration.
   If the argument is nil, then the default connection parameters
   are used."
  [cb-cfg]

  ;; force logging to use SLF4J facade
  (System/setProperty "net.spy.log.LoggerImpl" "net.spy.memcached.compat.log.SLF4JLogger")

  (log/info "create Couchbase client")
  (if-let [cfg (cfg/read-cfg cb-cfg)]
    (try
      (cbc/create-client cfg)
      (catch Exception e
        (log/error "error creating couchbase client" (str e))
        (cbc/create-client cb-client-defaults)))
    (do
      (log/warn "using default couchbase configuration")
      (cbc/create-client cb-client-defaults))))



