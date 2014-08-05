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

(ns eu.stratuslab.cimi.resources.user-template
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

(def ^:const resource-tag :userTemplates)

(def ^:const resource-name "UserTemplate")

(def ^:const collection-name "UserTemplateCollection")

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
;; UserTemplate schema
;;
(def UserTemplateType
  (s/enum "username-password"))

(def UserTemplate
  (merge c/CommonAttrs
         c/AclAttr
         {:user-type          UserTemplateType
          :first-name         c/NonBlankString
          :last-name          c/NonBlankString
          :username           c/NonBlankString
          :password-cleartext c/NonBlankString
          :password-confirm   c/NonBlankString
          :email              c/NonBlankString}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn UserTemplate))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

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

(def query-impl (crud/get-query-fn resource-name collection-acl collection-uri collection-name resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))
