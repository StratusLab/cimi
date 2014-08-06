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
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.volume-template :as vt]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.cb.views :as views]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [schema.core :as s]
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [cemerick.friend :as friend]))

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

(def ^:const create-uri (str c/cimi-schema-uri resource-name "Create")) ;; FIXME: NEEDED?

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

(def validate-create (u/create-validation-fn VolumeCreate)) ;; FIXME: NEEDED?

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Volume))
(defmethod c/validate resource-uri
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

(defn create-req->template [cb-client uri create-req]
  (let [skeleton (volume-skeleton uri create-req)
        volumeTemplate (:volumeTemplate create-req)
        volume-config (u/resolve-href cb-client (:volumeConfig volumeTemplate))
        volume-image (u/resolve-href cb-client (:volumeImage volumeTemplate))]
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
  [cb-client entry]
  (validate-create entry)
  (let [json entry                                          ;; FIXME: THIS IS WRONG!
        uri (str resource-name "/" (crud/new-identifier resource-name json))
        template (create-req->template cb-client uri entry)
        volume (template->volume template)
        params (template->params template)]
    (if (cbc/add-json cb-client uri volume)
      (let [job-resp (job/launch cb-client uri "create" params)]
        (if (= 202 (:status job-resp))
          (-> job-resp
              (r/status 201)
              (r/header "Location" uri))
          job-resp))
      (-> (str "cannot create " uri)
          (r/response)
          (r/status 400)))))

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
