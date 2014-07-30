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

(ns eu.stratuslab.cimi.cb.crud-utils
  "Basic utilities for handling CRUD actions for resources within the
   Couchbase database."
  (:require
    [clojure.tools.logging :as log]))

(defn create-resource
  "Creates a new resource within the cache using the :id key.
   Returns the resource that was passed in."
  [{:keys [id] :as resource}]
  nil)

(defn retrieve-resource
  "Retrieves the document associated with the given id."
  [id]
  nil)

(defn update-resource
  "Updates an existing resource within the cache using the :id key.
   Returns the resource that was passed in."
  [{:keys [id] :as resource}]
  nil)

(defn delete-resource
  "Deletes the credential information associated with the given id.  Returns nil."
  [id]
  nil)

(defn all-resource-ids
  "Returns a lazy sequence of all of the resource ids associated with the given
   resource type.  Internally this will chunk the queries to the underlying
   database."
  [resource-type]
  nil)

