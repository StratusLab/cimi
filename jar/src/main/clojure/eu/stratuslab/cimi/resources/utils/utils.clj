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

(ns eu.stratuslab.cimi.resources.utils.utils
  "General utilities for dealing with resources."
  (:require
    [eu.stratuslab.cimi.db.cb.views :as views]
    [clojure.walk :as w]
    [clojure.set :as set]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clj-time.core :as time]
    [clj-time.format :as time-fmt]
    [schema.core :as s]
    [ring.util.response :as r]
    [eu.stratuslab.cimi.db.dbops :as db])
  (:import
    [java.util UUID Date]))

(defn random-uuid
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

(defn update-timestamps
  "Sets the updated attribute and optionally the created attribute
   in the request.  The created attribute is only set if the existing value
   is missing or evaluates to false."
  [data]
  (let [updated (time-fmt/unparse (:date-time time-fmt/formatters) (time/now))
        created (or (:created data) updated)]
    (assoc data :created created :updated updated)))

(defn valid-timestamp?
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [data]
  (time-fmt/parse (:date-time time-fmt/formatters) data))

(defn body->json
  "Converts the contents of body (that must be something readable) into
   a clojure datastructure.  If the body is empty, then an empty map is
   returned."
  [body]
  (if body
    (cond
      (string? body) (json/read-str body :key-fn keyword :eof-error? false :eof-value {})
      (map? body) body
      :else (json/read (io/reader body) :key-fn keyword :eof-error? false :eof-value {}))))

(defn json->body
  "Converts a clojure data structure into a string."
  [json]
  (json/write-str json))

(defn correct-resource? [resource-uri resource]
  "Checks that the resourceURI attribute in the given resource matches
   the desired one."
  (= resource-uri (:resourceURI resource)))

(defn create-validation-fn
  "Creates a validation function that compares a resource against the
   given schema.  The generated function raises an exception with the
   violations of the schema and a 400 ring response. If everything's
   OK, then the resource itself is returned."
  [schema]
  (let [checker (s/checker schema)]
    (fn [resource]
      (if-let [msg (checker resource)]
        (let [msg (str "resource does not satisfy defined schema: " msg)
              response (-> (r/response msg)
                           (r/status 400))]
          (throw (ex-info msg response)))
        resource))))

(defn get-resource
  "Gets the resource identified by its URI from Couchbase.  If the URI is nil,
   this this returns an empty map.  If the URI doesn't exist in the database,
   then an exception is thrown."
  [cb-client uri]
  (if uri
    (if-let [json (cbc/get-json cb-client uri)]
      json
      (throw (ex-info (str "non-existent resource: " uri) {})))
    {}))

(defn service-configuration
  "Finds the service configuration document for the given service."
  [cb-client service-name]
  (->> service-name
       (str "ServiceConfiguration/")
       (cbc/get-json cb-client)))

(defn user-record
  "Finds a user record associated with a given identifier."
  [cb-client identifier]
  ;; FIXME: If there is more than one entry then code should fail.
  (let [opts {:include-docs true
              :key          identifier
              :limit        1
              :stale        false
              :on-error     :continue}
        q (cbq/create-query opts)
        v (views/get-view cb-client :user-ids)]
    (first (map cbc/view-doc-json (cbc/query cb-client v q)))))

(defn viewable-doc-ids
  "Returns a set of the document IDs of the given type of resource that are
   viewable by the given principal."
  [cb-client resource-type principal & [opts]]
  (let [default-opts {:include-docs false
                      :key          [resource-type principal]
                      :limit        100
                      :stale        false
                      :on-error     :continue}
        opts (merge default-opts opts)
        q (cbq/create-query opts)
        v (views/get-view cb-client :resource-type)]
    (set (map cbc/view-id (cbc/query cb-client v q)))))

(defn viewable-resources
  [cb-client resource-type principals & [opts]]
  (->> principals
       (map #(viewable-doc-ids cb-client resource-type % opts))
       (reduce (fn [s1 s2] (set/union s1 s2)))
       (cbc/get-multi-json cb-client)
       (vals)))

(defn resolve-href
  "If the given value is a map and contains the key :href, then the referenced
   resource is merged with the map (with the map values having priority).  The
   :href attribute itself is removed along with any common attributes."
  [v]
  (if (map? v)
    (if-let [uri (:href v)]
      (-> (db/retrieve uri)
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
  [v]
  (w/prewalk resolve-href v))

(defn bad-method
  "Returns a ring reponse with a 405 error -- invalid method."
  ([]
   (-> (r/response {:status  405
                    :message "invalid method"})
       (r/status 405)))
  ([{:keys [request-method uri]}]
   (-> (r/response {:status         405
                    :request-method request-method
                    :uri            uri
                    :message        (str "invalid method (" (name request-method) ") for " uri)})
       (r/status 405))))

(defn not-found
  "Returns a ring reponse with a 405 error -- invalid method."
  []
  (-> (r/response {:status 404 :message "not found"})
      (r/status 404)))

(defn unauthorized
  "Returns a ring reponse with a 403 error -- unauthorized."
  ([]
   (-> (r/response {:status 403 :message "unauthorized"})
       (r/status 403)))
  ([{:keys [request-method uri]}]
   (-> (r/response {:status         403
                    :request-method request-method
                    :uri            uri
                    :message        (str "unauthorized (" (name request-method) ") for " uri)})
       (r/status 403))))

(defn conflict
  "Returns a ring reponse with a 409 error -- conflict."
  ([]
   (-> (r/response {:status 409 :message "conflict"})
       (r/status 409)))
  ([{:keys [request-method uri]}]
   (-> (r/response {:status         409
                    :request-method request-method
                    :uri            uri
                    :message        (str "conflict (" (name request-method) ") for " uri)})
       (r/status 409))))

(defn ex-response
  [msg code {:keys [request-method uri] :as request}]
  (let [body {:status code
              :request-method (name (or request-method "unknown"))
              :uri (or uri "unknown")
              :message msg}
        resp (-> (r/response body)
                 (r/status code))]
    (ex-info msg resp)))

(defn ex-client
  ([{:keys [request-method uri] :as request}]
   (ex-client request (str "bad request (" (name request-method) ") for " uri)))

  ([{:keys [request-method uri] :as request} msg]
   (ex-response msg 400 request)))

(defn ex-unauthorized
  [{:keys [request-method uri] :as request}]
  (-> (str "unauthorized (" (name request-method) ") for " uri)
      (ex-response 403 request)))

(defn ex-not-found
  [{:keys [request-method uri] :as request}]
  (-> (str  uri "not found (" (name request-method) ")")
      (ex-response 404 request)))

(defn ex-bad-method
  [{:keys [request-method uri] :as request}]
  (-> (str "unsupported method (" (name request-method) ") for " uri)
      (ex-response 405 request)))

(defn ex-conflict
  [{:keys [request-method uri] :as request}]
  (-> (str "conflict (" (name request-method) ") for " uri)
      (ex-response 409 request)))

