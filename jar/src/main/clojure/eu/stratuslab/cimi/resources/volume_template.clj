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
(ns eu.stratuslab.cimi.resources.volume-template
  "Template combines the characteristics of the Volume to be created
   through the referenced (or embedded) VolumeConfiguration and the source
   for initializing the Volume through the referenced (or embedded)
   VolumeImage."
  (:require
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.common.schema :as c]
    [eu.stratuslab.cimi.resources.common.crud :as crud]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.volume-configuration :as vc]
    [eu.stratuslab.cimi.resources.volume-image :as vi]
    [eu.stratuslab.cimi.resources.event-log-template :as el]
    [eu.stratuslab.cimi.resources.meter-template :as mt]
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [cemerick.friend :as friend]))

(def ^:const resource-tag :volumeTemplates)

(def ^:const resource-name "VolumeTemplate")

(def ^:const collection-name "VolumeTemplateCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
;;
;; schemas
;;

(def VolumeTemplate
  (merge c/CommonAttrs
         c/AclAttr
         {:volumeConfig                      vc/VolumeConfigurationRef
          (s/optional-key :volumeImage)      vi/VolumeImageRef
          (s/optional-key :meterTemplates)   mt/MeterTemplateRefs
          (s/optional-key :eventLogTemplate) el/EventLogTemplateRef}))

(def VolumeTemplateAttrs
  {(s/optional-key :volumeConfig)     vc/VolumeConfigurationRef
   (s/optional-key :volumeImage)      vi/VolumeImageRef
   (s/optional-key :meterTemplates)   mt/MeterTemplateRefs
   (s/optional-key :eventLogTemplate) el/EventLogTemplateRef})

(def VolumeTemplateRef
  (s/both
    (merge VolumeTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    c/NotEmpty))


;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn VolumeTemplate))
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

(def query-impl (crud/get-query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))
