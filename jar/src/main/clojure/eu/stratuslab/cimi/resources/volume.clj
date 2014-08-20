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
(ns eu.stratuslab.cimi.resources.volume
  "Utilities for managing the CRUD features for volumes."
  (:require
    [eu.stratuslab.cimi.resources.common.schema :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.volume-template :as vt]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.resources.common.schema :as c]
    [eu.stratuslab.cimi.resources.common.crud :as crud]
    [ring.util.response :as r]
    [schema.core :as s]
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [pprint]]
    [cemerick.friend :as friend]
    [eu.stratuslab.cimi.db.dbops :as db]))

(def ^:const resource-tag :volumes)

(def ^:const resource-name "Volume")

(def ^:const collection-name "VolumeCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

(def ^:const create-uri (str c/cimi-schema-uri resource-name "Create"))

(def job-acl {:owner {:principal "::ADMIN"
                      :type      "ROLE"}})

;;
;; schemas
;;

(def volume-states (s/enum "CREATING" "AVAILABLE" "CAPTURING" "DELETING" "ERROR"))

(def Volume
  (merge c/CommonAttrs
         c/AclAttr
         {(s/optional-key :state)    volume-states
          :type                      c/NonBlankString
          :capacity                  c/PosInt
          (s/optional-key :bootable) s/Bool
          (s/optional-key :eventLog) c/ResourceLink}))

(def VolumeCreate
  (merge c/CreateAttrs
         ;; c/AclAttr  ;; FIXME: does create need an ACL at this level?
         {:volumeTemplate vt/VolumeTemplateRef}))

(def validate-create (u/create-validation-fn VolumeCreate))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Volume))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn VolumeCreate))
(defmethod c/validate create-uri
           [resource]
  (create-validate-fn resource))

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (a/add-acl resource (friend/current-authentication)))

;;
;; functions for special handling of templates
;;

(defn volume-skeleton
  [uuid type capacity bootable]
  (let [id (str resource-name "/" uuid)
        event-log (str "EventLog/" uuid)]
    {:id          id
     :resourceURI resource-uri
     :state       "CREATING"
     :type        type
     :capacity    capacity
     :bootable    (boolean bootable)
     :eventLog    {:href event-log}}))

;; FIXME: Create real job!
(defn volume-job
  [uuid state imageLocation format]
  {:id             (str "Job/" uuid)
   :properties     {"state"      state
                    "image-href" (:href imageLocation)
                    "format"     format}
   :targetResource (str "Volume/" uuid)
   :action         "create"
   :acl            job-acl})

;;
;; CRUD operations
;;

(defn process-template
  "Accepts a VolumeTemplate and returns a vector with the initialized
   Volume and a Job."
  [tpl]
  (let [uuid (u/random-uuid)
        {:keys [volumeConfig volumeImage] :as volume-template} (u/resolve-href tpl)
        {:keys [type format capacity] :as volumeConfig} (u/resolve-href volumeConfig)
        {:keys [state imageLocation bootable] :as volumeImage} (u/resolve-href volumeImage)]
    [(volume-skeleton uuid type capacity bootable)
     (volume-job uuid state imageLocation format)]))

(defn dump [m]
  (pprint m)
  m)

(defn add-impl
  [{:keys [body] :as request}]
  (a/modifiable? {:acl collection-acl} request)
  (let [[volume job] (-> (u/body->json body)
                         (assoc :resourceURI create-uri)
                         (c/validate)
                         (:volumeTemplate)
                         (process-template))]
    (-> volume
        (u/update-timestamps)
        (crud/add-acl resource-name)
        (c/validate)
        (db/add))

    (let [id (:id volume)
          response (job/launch job)]
      (if (= 202 (:status response))
        (-> response
            (r/status 201)
            (r/header "Location" id))
        response))))

;; requires a VolumeTemplate to create new Volume
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
