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
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.common.schema :as c]
    [eu.stratuslab.cimi.resources.common.crud :as crud]
    [ring.util.response :as r]
    [schema.core :as s]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :users)

(def ^:const resource-name "User")

(def ^:const collection-name "UserCollection")

(def ^:const resource-uri (str c/stratuslab-cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/stratuslab-cimi-schema-uri collection-name))

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

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (let [acl {:owner {:principal "::ADMIN" :type "ROLE"}
             :rules [{:principal (:username resource) :type "USER" :right "VIEW"}]}]
    (assoc resource :acl acl)))

;;
;; User resources use the username as the record identifier
;; and not a UUID.
;;
(defmethod crud/new-identifier resource-name
           [resource-name json]
  (str resource-name "/" (:username json)))

;;
;; CRUD operations
;;

(def add-impl (crud/get-add-fn resource-name collection-acl resource-uri))

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

(def query-impl (crud/get-query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))
