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
(ns eu.stratuslab.cimi.resources.volume-image
  "A CIMI VolumeImage corresponds to the images in the StratusLab Marketplace.
   These are images that can be used to initialize volumes within the cloud
   infrastructure (either machine or data images).

   NOTE: Unlike for other resources, the unique identifier (UUID) for
   VolumeImages are the base64-encoded SHA-1 hash of the image."
  (:require
    [eu.stratuslab.cimi.resources.common.schema :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.common.crud :as crud]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [cemerick.friend :as friend]))

(def ^:const resource-tag :volumeImages)

(def ^:const resource-name "VolumeImage")

(def ^:const collection-name "VolumeImageCollection")

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

(def image-states (s/enum "CREATING" "AVAILABLE" "DELETING" "ERROR"))

(def VolumeImage
  (merge c/CommonAttrs
         c/AclAttr
         {:state         image-states
          :imageLocation c/ResourceLink
          :bootable      s/Bool}))

(def VolumeImageAttrs
  {(s/optional-key :state)         image-states
   (s/optional-key :imageLocation) c/ResourceLink
   (s/optional-key :bootable)      s/Bool})

(def VolumeImageRef
  (s/both
    (merge VolumeImageAttrs
           {(s/optional-key :href) c/NonBlankString})
    c/NotEmpty))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn VolumeImage))
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
