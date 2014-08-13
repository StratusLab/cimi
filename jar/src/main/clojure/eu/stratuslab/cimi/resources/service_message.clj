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
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]))

;;
;; utilities
;;

(def ^:const resource-tag :serviceMessages)

(def ^:const resource-name "ServiceMessage")

(def ^:const collection-name "ServiceMessageCollection")

(def ^:const resource-uri (str c/stratuslab-cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/stratuslab-cimi-schema-uri collection-name))

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

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (assoc resource :acl {:owner {:principal "::ADMIN"
                                :type      "ROLE"}
                        :rules [{:principal "::ANON"
                                 :type      "ROLE"
                                 :right     "VIEW"}]}))

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
