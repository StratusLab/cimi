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

(ns eu.stratuslab.cimi.db.dbops
  (:require
    [eu.stratuslab.cimi.db.protocol :as p]
    [clojure.tools.logging :as log]))

(def ^:dynamic *dbops*)

(defn set-impl!
  [dbops]
  (alter-var-root #'*dbops* (constantly dbops)))

(defn bootstrap []
  (try
    (p/bootstrap *dbops*)
    (catch Exception e
      (log/error "error bootstrapping database operations protocol: " (str e)))))

(defn close []
  (try
    (p/close *dbops*)
    (catch Exception e
      (log/error "error closing database operations protocol: " (str e)))))

(defn add [resource]
  (p/add *dbops* resource))

(defn retrieve [id]
  (p/retrieve *dbops* id))

(defn edit [resource]
  (p/edit *dbops* resource))

(defn delete [resource]
  (p/delete *dbops* resource))

(defn query [collection-id options]
  (p/query *dbops* collection-id options))
