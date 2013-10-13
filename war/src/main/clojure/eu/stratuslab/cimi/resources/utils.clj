;
; Copyright 2013 Centre National de la Recherche Scientifique (CNRS)
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
(ns eu.stratuslab.cimi.resources.utils
  "General utilities for dealing with resources."
  (:require
    [clojure.walk :as w]
    [couchbase-clj.client :as cbc]
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clj-time.core :as time]
    [clj-time.format :as time-fmt]
    [clj-schema.validation :refer [validation-errors]]
    [ring.util.response :as r])
  (:import
    [java.util UUID Date]))

(defn create-uuid
  "Provides the string representation of a pseudo-random UUID."
  []
  (str (UUID/randomUUID)))

(defn strip-common-attrs
  "Strips all common resource attributes from the map."
  [m]
  (dissoc m :id :name :description :created :updated :properties))

(defn strip-service-attrs
  "Strips common attributes from the map whose values are controlled
   entirely by the service.  These include :id, :created, :updated, 
   :resourceURI, and :operations."
  [m]
  (dissoc m :id :created :updated :resourceURI :operations))

(defn set-time-attributes
  "Sets the updated attribute and optionally the created attribute
   in the request.  The created attribute is only set if the existing value
   is missing or evaluates to false."
  [data]
  (let [updated (time-fmt/unparse (:date-time time-fmt/formatters) (time/now))
        created (or (:created data) updated)]
    (assoc data :created created :updated updated)))

(defn body->json
  "Converts the contents of body (that must be something readable) into
   a clojure datastructure.  If the body is empty, then an empty map is
   returned."
  [body]
  (if body
    (json/read (io/reader body) :key-fn keyword :eof-error? false :eof-value {})
    {}))

(defn correct-resource? [resource-uri resource]
  "Checks that the resourceURI attribute in the given resource matches
   the desired one."
  (= resource-uri (:resourceURI resource)))

(defn create-validation-fn
  "Creates a validation function that compares a resource against the
   given schema.  The generated function raises an exception with the 
   violations of the schema or the resource itself if everything's OK."
  [schema]
  (fn [resource]
    (let [errors (validation-errors schema resource)]
      (if (empty? errors)
        resource
        (throw (Exception. (str "resource does not satisfy defined schema\n"
                                (str/join "\n" errors))))))))

(defn get-resource
  "Gets the resource identified by its URI from Couchbase.  If the URI is nil,
   this this returns an empty map.  If the URI doesn't exist in the database,
   then an exception is thrown."
  [cb-client uri]
  (if uri
    (if-let [json (cbc/get-json cb-client uri)]
      json
      (throw (Exception. (str "non-existent resource: " uri))))
    {}))

(defn resolve-href
  "If the given value is a map and contains the key :href, then the referenced
   resource is merged with the map (with the map values having priority).  The
   :href attribute itself is removed along with any common attributes."
  [cb-client v]
  (if (map? v)
    (if-let [uri (:href v)]
      (-> (get-resource cb-client uri)
          (merge v)
          (dissoc :href)
          (strip-common-attrs))
      v)
    v))

(defn resolve-hrefs
  "Does a prewalk of the given data structure, replacing any map with an href
   attribute with the result of merging the referenced resource with the 
   values provided locally.  If a reference is found, the common attributes
   are also removed from the map."
  [cb-client v]
  (w/prewalk (partial resolve-href cb-client) v))

(defn bad-method
  "Returns a ring reponse with a 405 error -- invalid method."
  []
  (-> (r/response nil)
      (r/status 405)))

(defn unauthorized
  "Returns a ring reponse with a 403 error -- unauthorized."
  []
  (-> (r/response nil)
      (r/status 403)))

