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
    (let [ops [{:rel (:add schema/action-uri) :href resource-name}]]
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

;;
;; User resources use the username as the record identifier
;; and not a UUID.
;;
(defmethod crud/new-identifier resource-name
           [resource-name json]
  (:username json))

;;
;; CRUD operations
;;

(def add-impl (crud/get-add-fn resource-name collection-acl resource-uri add-acl))

(defmethod crud/add resource-name
           [request]
  (add-impl request))

(def retrieve-impl (crud/get-retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
           [request]
  (retrieve-impl request))

(def edit-impl (crud/get-edit-fn resource-name))

(defmethod crud/edit resource-name
           [request]
  (edit-impl request))

(def delete-impl (crud/get-delete-fn resource-name))

(defmethod crud/delete resource-name
           [request]
  (delete-impl request))

(def query-impl (crud/get-query-fn resource-name collection-acl collection-uri collection-name resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))
