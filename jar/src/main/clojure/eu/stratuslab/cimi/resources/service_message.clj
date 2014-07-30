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
(ns eu.stratuslab.cimi.resources.service-message
  "Allows administrators to provide general service messages for users.
   It is recommended that the message be formatted in Markdown format."
  (:require
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.impl.schema :as schema]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.cb.views :as views]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]))

;;
;; utilities
;;

(def ^:const resource-tag :serviceMessages)

(def ^:const resource-name "ServiceMessage")

(def ^:const collection-resource-name "ServiceMessageCollection")

(def ^:const resource-uri (str "http://stratuslab.eu/cimi/1/" resource-name))

(def ^:const collection-resource-uri (str "http://stratuslab.eu/cimi/1/" collection-resource-name))

(def ^:const base-uri (str "/cimi/" resource-name))

(def ^:const collection-acl {:owner {:principal "::ADMIN"
                                     :type      "ROLE"}
                             :rules [{:principal "::ANON"
                                      :type      "ROLE"
                                      :right     "VIEW"}]})

(defn uuid->id
  [uuid]
  (str resource-name "/" uuid))

;;
;; ServiceMessage schema
;;

(def ServiceMessage
  (merge c/CommonAttrs
         c/AclAttr
         {:title   c/NonBlankString
          :message c/NonBlankString}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn ServiceMessage))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(defmethod c/set-operations resource-uri
           [resource]
  (if (a/can-modify? (:acl resource))
    (let [href (:id resource)
          ops [{:rel (:edit schema/action-uri) :href href}
               {:rel (:delete schema/action-uri) :href href}]]
      (assoc resource :operations ops))
    (dissoc resource :operations)))

(defmethod c/set-operations collection-resource-uri
           [resource]
  (if (a/can-modify? collection-acl)
    (let [ops [{:rel (:add schema/action-uri) :href base-uri}]]
      (assoc resource :operations ops))
    resource))

;;
;; special method
;;

(defn add-acl [resource]
  (assoc resource :acl {:owner {:principal "::ADMIN"
                                :type      "ROLE"}
                        :rules [{:principal "::ANON"
                                 :type      "ROLE"
                                 :right     "VIEW"}]}))

;;
;; CRUD operations
;;

(defn add
  "Add a new ServiceMessage to the database."
  [cb-client entry]
  (let [uri (uuid->id (u/random-uuid))
        entry (-> entry
                  (u/strip-service-attrs)
                  (assoc :id uri)
                  (assoc :resourceURI resource-uri)
                  (u/set-time-attributes)
                  (add-acl)                                 ;; special ACL for these messages
                  (c/validate))]
    (if (cbc/add-json cb-client uri entry)
      (r/created uri)
      (r/status (r/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the data associated with the requested ServiceMessage
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->id uuid))]
    (if (a/can-view? (:acl json))
      (r/response (c/set-operations json))
      (u/unauthorized))
    (r/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->id uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (:acl current))
        (let [updated (->> entry
                           (u/strip-service-attrs)
                           (merge current)
                           (u/set-time-attributes)
                           (c/validate))]
          (if (cbc/set-json cb-client uri updated)
            (r/response updated)
            (r/status (r/response nil) 409)))               ;; conflict
        (u/unauthorized))
      (r/not-found nil))))

(defn delete
  "Deletes the ServiceMessage."
  [cb-client uuid]
  (let [uri (uuid->id uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (:acl current))
        (if (cbc/delete cb-client uri)
          (r/response nil)
          (r/not-found nil))
        (u/unauthorized))
      (r/not-found nil))))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [principals (a/authn->principals)
        configs (u/viewable-resources cb-client resource-name principals opts)
        configs (map c/set-operations configs)
        collection (c/set-operations {:resourceURI collection-resource-uri
                                      :id          base-uri
                                      :count       (count configs)})]
    (r/response (if (empty? collection)
                  collection
                  (assoc collection :serviceMessages configs)))))

;;
;; function bindings for compojure routes
;;

(defmethod crud/add resource-name
           [_ cb-client body]
  (if (a/can-modify? collection-acl)
    (let [json (u/body->json body)]
      (add cb-client json))
    (u/unauthorized)))

(defmethod crud/query resource-name
           [_ cb-client body]
  (if (a/can-view? collection-acl)
    (let [json (u/body->json body)]
      (query cb-client json))
    (u/unauthorized)))

(defmethod crud/retrieve resource-name
           [_ cb-client uuid]
  (retrieve cb-client uuid))

(defmethod crud/edit resource-name
           [_ cb-client uuid body]
  (let [json (u/body->json body)]
    (edit cb-client uuid json)))

(defmethod crud/delete resource-name
           [_ cb-client uuid]
  (delete cb-client uuid))
