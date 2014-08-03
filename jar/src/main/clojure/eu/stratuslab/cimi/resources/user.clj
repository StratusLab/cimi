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

(ns eu.stratuslab.cimi.resources.user
  "Management of users within the CIMI framework.  This is a StratusLab
   extension.  It manages only user records within the database.  Users
   from external sources (LDAP, VOMS proxies, etc.) are not managed by
   these resources."
  (:require
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [clojure.data.json :as json]
    [eu.stratuslab.cimi.resources.impl.schema :as schema]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.cb.views :as views]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [cemerick.friend :as friend]
    [schema.core :as s]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :users)

(def ^:const resource-name "User")

(def ^:const collection-name "UserCollection")

(def ^:const resource-uri (str c/stratuslab-cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/stratuslab-cimi-schema-uri collection-name))

(def ^:const base-uri (str c/service-context resource-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(defn uuid->id
  [uuid]
  (str resource-name "/" uuid))

;;
;; User schema
;;
(def Roles
  (s/both
    [c/NonBlankString]
    c/NotEmpty))

(def Altnames
  (s/both
    {s/Keyword c/NonBlankString}
    c/NotEmpty))

(def User
  (merge c/CommonAttrs
         c/AclAttr
         {:first-name                c/NonBlankString
          :last-name                 c/NonBlankString
          :username                  c/NonBlankString
          (s/optional-key :password) c/NonBlankString
          (s/optional-key :enabled)  s/Bool
          (s/optional-key :roles)    Roles
          (s/optional-key :altnames) Altnames
          :email                     c/NonBlankString}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn User))
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

(defmethod c/set-operations collection-uri
           [resource]
  (if (a/can-modify? collection-acl)
    (let [href (:id resource)
          ops [{:rel (:add schema/action-uri) :href href}]]
      (assoc resource :operations ops))
    (dissoc resource :operations)))


;;
;; special method
;;

(defn add-acl
  "ACL allowing the users to view but not modify their entries."
  [m]
  (let [acl {:owner {:principal "::ADMIN" :type "ROLE"}
             :rules [{:principal (:username m) :type "USER" :right "VIEW"}]}]
    (assoc m :acl acl)))

(defn dump-entry [m]
  (pprint m)
  m)

;;
;; CRUD operations
;;

(defn add
  "Adds a new user to the database."
  [cb-client entry]
  (let [uri (uuid->id (:username entry))
        entry (-> entry
                  (u/strip-service-attrs)
                  (assoc :id uri)
                  (assoc :resourceURI resource-uri)
                  (u/update-timestamps)
                  (add-acl)
                  (c/validate))]
    (if (cbc/add-json cb-client uri entry)
      (r/created uri)
      (r/status (r/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the user record associated with the user's identity."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->id uuid))]
    (if (a/can-view? (friend/current-authentication) (:acl json))
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
      (if (a/can-modify? (friend/current-authentication) (:acl current))
        (let [updated (->> entry
                           (u/strip-service-attrs)
                           (merge current)
                           (u/update-timestamps)
                           (c/set-operations)
                           (c/validate))]
          (if (cbc/set-json cb-client uri updated)
            (r/response updated)
            (r/status (r/response nil) 409)))               ;; conflict
        (u/unauthorized))
      (r/not-found nil))))

(defn delete
  "Deletes the named user record."
  [cb-client uuid]
  (let [uri (uuid->id uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (friend/current-authentication) (:acl current))
        (if (cbc/delete cb-client uri)
          (r/response nil)
          (r/not-found nil))
        (u/unauthorized))
      (r/not-found nil))))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [principals (a/authn->principals (friend/current-authentication))
        configs (u/viewable-resources cb-client resource-name principals opts)
        configs (map c/set-operations configs)
        collection (c/set-operations {:resourceURI collection-uri
                                      :id          collection-name
                                      :count       (count configs)})]
    (r/response (if (empty? collection)
                  collection
                  (assoc collection :users configs)))))

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
