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

(ns eu.stratuslab.cimi.app.main
  "Entry point for running CIMI server from the command line and
   hence from system service management scripts."
  (:require [eu.stratuslab.cimi.app.server :refer [register-shutdown-hook start]])
  (:gen-class))

(defn valid-port?
  "If the port number is valid, then returns the port itself;
   otherwise returns nil."
  [port]
  (if (< 0 port 65536)
    port))

(defn parse-port
  "Parses the given string into a port value.  If the port is not
   valid, then function returns nil."
  [s]
  (try
    (valid-port? (Integer/valueOf s))
    (catch Exception e
      nil)))

(defn -main
  "Starts the cimi server using the command line arguments.  Takes as
   possible arguments the port number and Couchbase configuration file."
  [& [port cbcfg]]
  (let [port (or (parse-port port) 9200)
        cbcfg (or cbcfg "/etc/stratuslab/couchbase.cfg")]
    (->> (start port cbcfg)
         (register-shutdown-hook))))
