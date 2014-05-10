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

(ns eu.stratuslab.cimi.main
  "Entry point for running CIMI server from the command line and
   hence from system service management scripts."
  (:gen-class)
  (:require
    [eu.stratuslab.cimi.server :refer [start register-shutdown-hook]]
    [clojure.tools.logging :as log]))

(defn parse-port
  [s]
  (try
    (let [port (Integer/valueOf s)]
      (if (< 0 port 65536)
        port
        nil))
    (catch Exception e
      nil)))

(defn -main
  "Starts the cimi server using the command line arguments.  Takes as
   possible arguments the port number, Couchbase configuration file, and
   the context."
  [& [port cbcfg context]]
  (let [port (or (parse-port port) 9200)
        cbcfg (or cbcfg "/etc/stratuslab/couchbase.cfg")
        context (or context "cimi")]
    (->> (start port cbcfg context)
         (register-shutdown-hook))))
