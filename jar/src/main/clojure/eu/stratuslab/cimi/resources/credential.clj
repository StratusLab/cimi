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

(ns eu.stratuslab.cimi.resources.credential
  "Management functions for credential resources within the database."
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.cb.crud-utils :as db]))

;;
;; utilities
;;

(def ^:const resource-type "Credential")

(def ^:const resource-type-uri "http://schemas.dmtf.org/cimi/1/Credential")

(def ^:const resource-template-type-uri "http://schemas.dmtf.org/cimi/1/CredentialTemplate")

(defn uuid->id
  [uuid]
  (str resource-type "/" uuid))

;;
;; credential schema
;;

(def CommonCredentialAttributes
  {:subtypeURI              s/Str
   (s/optional-key :expiry) s/Int})

(def Credential
  (merge c/CommonAttrs CommonCredentialAttributes))

(def CredentialTemplate
  (merge c/CommonAttrs CommonCredentialAttributes))

;;
;; standard CRUD functions for credentials
;;

(defn create
  "Adds a new credential to the database given the information in the
   template; returns the id of the created credential."
  [template]
  (let [resource (-> template
                     (c/validate-template)
                     (c/template->resource))]
    (->> (u/random-uuid)
         (uuid->id)
         (assoc resource :id)
         (u/update-timestamps)
         (c/validate)
         (db/create-resource)
         (:id))))

(defn retrieve
  [id]
  (db/retrieve-resource id))

(defn update
  "Updates an existing credential in the database.  The identifier
   must be part of the credential itself under the :id key. Returns
   the updated resource."
  [resource]
  (->> resource
       (u/update-timestamps)
       (c/validate)
       (db/update-resource)))

(defn delete
  "Removes the credential associated with the id from the database."
  [id]
  (db/delete-resource id))

(defn resource-ids
  "Provides a list of all of the credential ids in the database."
  []
  (db/all-resource-ids resource-type))
