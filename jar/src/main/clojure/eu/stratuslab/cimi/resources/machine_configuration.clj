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

(ns eu.stratuslab.cimi.resources.machine-configuration
  "Utilities for managing the CRUD features for machine configurations."
  (:require
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [ring.util.response :as r]
    [cemerick.friend :as friend]
    [schema.core :as s]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :machineConfigs)

(def ^:const resource-name "MachineConfiguration")

(def ^:const collection-name "MachineConfigurationCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; MachineConfiguration
;;

(def ValidCpuArch
  (s/enum "68000" "Alpha" "ARM" "Itanium" "MIPS" "PA_RISC"
          "POWER" "PowerPC" "x86" "x86_64" "zArchitecture", "SPARC"))

(def Disk
  {:capacity                         c/PosInt
   :format                           c/NonBlankString
   (s/optional-key :initialLocation) c/NonBlankString})

(def Disks
  (s/both [Disk] c/NotEmpty))

(def MachineConfiguration
  (merge c/CommonAttrs
         c/AclAttr
         {:cpu                    c/PosInt
          :memory                 c/PosInt
          :cpuArch                ValidCpuArch
          (s/optional-key :disks) Disks}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn MachineConfiguration))
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
