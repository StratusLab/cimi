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
    [eu.stratuslab.cimi.server :refer [start]]
    [clojure.tools.logging :as log]))

(defn -main
  "Starts the cimi server using the command line arguments.  The
   recognized arguments are the port and the name of the Couchbase
   configuration file."
  [& args]
  (start 8080))
