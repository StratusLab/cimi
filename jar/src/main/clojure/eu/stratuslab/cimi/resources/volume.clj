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
          (s/optional-key :eventLog) c/NonBlankString}))

(def VolumeCreate
  (merge c/CreateAttrs
         c/AclAttr
         {:volumeTemplate vt/VolumeTemplateRef}))

(def validate-create (u/create-validation-fn VolumeCreate))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Volume))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(def validate-fn (u/create-validation-fn VolumeCreate))
(defmethod c/validate create-uri
           [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (a/add-acl resource (friend/current-authentication)))

;;
;; functions for special handling of templates
;;

(defn volume-skeleton [uri entry]
  (if (u/correct-resource? create-uri entry)
    (-> entry
        (u/strip-service-attrs)
        (dissoc :volumeTemplate)
        (assoc :resourceURI resource-uri)
        (u/update-timestamps)
        (assoc :state "CREATING" :id uri))
    (throw (Exception. (str create-uri " resource required")))))

(defn create-req->template [uri create-req]
  (let [skeleton (volume-skeleton uri create-req)
        volumeTemplate (:volumeTemplate create-req)
        volume-config (u/resolve-href (:volumeConfig volumeTemplate))
        volume-image (u/resolve-href (:volumeImage volumeTemplate))]
    (merge volume-config volume-image skeleton)))

(defn template->volume [template]
  (-> (select-keys template [:id :resourceURI :name :description
                             :created :updated :properties
                             :state :type :capacity
                             :bootable :images :meters :eventLog])
      (assoc :acl {:owner {:principal "::ADMIN" :type "ROLE"}}) ;; FIXME: Add real user ACL!
      (c/validate)))

(defn template->params [template]
  (let [params (select-keys template [:type :format :capacity :bootable])]
    (if-let [imageLocation (get-in template [:imageLocation :href])]
      (assoc params :imageLocation imageLocation)
      params)))

(defn add
  "Add a new Volume to the database based on the VolumeTemplate
   passed into this method."
  [entry]
  (validate-create entry)
  (let [json entry                                          ;; FIXME: THIS IS WRONG!
        uri (crud/new-identifier resource-name json)
        template (create-req->template uri entry)
        volume (template->volume template)
        params (template->params template)]
    (db/add volume)
    (let [job-resp (job/launch uri "create" params)]
      (if (= 202 (:status job-resp))
        (-> job-resp
            (r/status 201)
            (r/header "Location" uri))
        job-resp))))

;;
;; CRUD operations
;;

(defn correct-resource-uri?
  [{:keys [resourceURI] :as resource} expected-resource-uri]
  (if (= resourceURI expected-resource-uri)
    resource
    (let [msg (str "resourceURI mismatch: " expected-resource-uri " (expected) != " resourceURI " (actual)")
          resp (-> (r/response msg)
                   (r/status 400))]
      (throw (ex-info msg resp)))))

(defn create->template [create-tpl]
  (let [skeleton (volume-skeleton (:id create-tpl) create-tpl)
        volumeTemplate (:volumeTemplate create-tpl)
        volume-config (u/resolve-href (:volumeConfig volumeTemplate))
        volume-image (u/resolve-href (:volumeImage volumeTemplate))]
    (merge volume-config volume-image skeleton)))

(defn add-impl [{:keys [body] :as request}]
  (a/modifiable? {:acl collection-acl} request)
  (let [json (u/body->json body)
        uri (crud/new-identifier resource-name json)]
    (-> json
        (correct-resource-uri? create-uri)
        (create->template)
        (u/strip-service-attrs)
        (assoc :id uri)
        (assoc :resourceURI resource-uri)
        (u/update-timestamps)
        (crud/add-acl resource-name)
        (c/validate)
        (db/add))))

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
