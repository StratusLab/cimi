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

(ns eu.stratuslab.cimi.resources.service-configuration
  "Management of the configuration of the cloud services."
  (:require
    [clojure.string :as str]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [clojure.data.json :as json]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.cb.views :as views]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [cemerick.friend :as friend]
    [schema.core :as s]
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]))

(def ^:const resource-tag :serviceConfigurations)

(def ^:const resource-name "ServiceConfiguration")

(def ^:const collection-name "ServiceConfigurationCollection")

(def ^:const resource-uri (str c/stratuslab-cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/stratuslab-cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}})

(def resource-acl {:owner {:principal "::ADMIN"
                           :type      "ROLE"}})

;;
;; Service configuration files.  (StratusLab extension.)
;;
(def ServiceConfiguration
  (merge c/CommonAttrs
         c/AclAttr
         {:service                   c/NonBlankString
          (s/optional-key :instance) c/NonBlankString
          s/Keyword                  s/Any}))

;;
;; multimethods for validation and operations
;;

(defmethod crud/new-identifier resource-name
           [resource-name {:keys [service instance] :or {instance "default"}}]
  (str service "." instance))

(def validate-fn (u/create-validation-fn ServiceConfiguration))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (assoc resource :acl resource-acl))

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
