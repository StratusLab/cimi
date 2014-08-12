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

(ns eu.stratuslab.cimi.resources.credential-template
  "Credential templates for creating new credentials."
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [cemerick.friend :as friend]))

(def ^:const resource-tag :credentialTemplates)

(def ^:const resource-name "CredentialTemplate")

(def ^:const collection-name "CredentialTemplateCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; schemas
;;

(def UsernamePasswordCredential
  {:username c/NonBlankString
   :password c/NonBlankString})

(def SSHPublicKeyCredential
  {:publicKey c/NonBlankString})

(def CredentialTemplate
  (s/either
    (merge c/CommonAttrs c/AclAttr UsernamePasswordCredential)
    (merge c/CommonAttrs c/AclAttr SSHPublicKeyCredential)))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn CredentialTemplate))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (a/add-acl resource (friend/current-authentication)))

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
