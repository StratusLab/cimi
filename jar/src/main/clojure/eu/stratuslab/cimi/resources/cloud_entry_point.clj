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
(ns eu.stratuslab.cimi.resources.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.utils.dynamic-load :as dyn]
    [compojure.core :refer [defroutes GET PUT ANY]]
    [ring.util.response :as r]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [eu.stratuslab.cimi.db.dbops :as db]))

;;
;; utilities
;;

(def ^:const resource-name "CloudEntryPoint")

(def ^:const resource-uri (str "http://schemas.dmtf.org/cimi/1/" resource-name))

(def ^:const base-uri "/cimi")

;; FIXME: Determine if add-acl method should be implemented and used for CEP.
(def resource-acl {:owner {:principal "::ADMIN" :type "ROLE"}
                   :rules [{:principal "::ANON" :type "ROLE" :right "VIEW"}]})

;;
;; CloudEntryPoint Schema
;;

(def CloudEntryPoint
  (merge c/CommonAttrs
         c/AclAttr
         {:baseURI  c/NonBlankString
          s/Keyword c/ResourceLink}))

;; dynamically loads all available resources
(def resource-links
  (into {} (dyn/get-resource-links)))

(def stripped-keys
  (concat (keys resource-links) [:baseURI :operations]))

;;
;; define validation function and add to standard multi-method
;;

(def validate-fn (u/create-validation-fn CloudEntryPoint))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))


(defmethod c/set-operations resource-uri
           [resource request]
  (try
    (a/modifiable? resource request)
    (assoc resource :operations [{:rel (:edit c/action-uri) :href "#"}])
    (catch Exception e
      (dissoc resource :operations))))

;;
;; CRUD operations
;;

(defn add
  "Creates a minimal CloudEntryPoint in the database.  Note that
   only the common attributes are saved in the database; links to
   resource types are generated when the service starts.

   NOTE: Unlike other resources, the :id is 'CloudEntryPoint'
   rather than the relative URI for the resource."
  []
  (let [record (-> {:acl         resource-acl
                    :id          resource-name
                    :resourceURI resource-uri}
                   (u/update-timestamps))]
    (db/add record)))

(defn retrieve-impl
  [{:keys [baseURI] :as request}]
  (a/viewable? {:acl resource-acl} request)
  (let [cep (db/retrieve resource-name)]
    (r/response (-> cep
                    (assoc :baseURI baseURI)
                    (merge resource-links)
                    (c/set-operations request)))))

(defmethod crud/retrieve resource-name
           [request]
  (retrieve-impl request))

(defn edit-impl
  [{:keys [body] :as request}]
  (let [current (-> (db/retrieve resource-name)
                    (a/modifiable? request))]
    (let [new (-> (u/body->json body)
                  (assoc :baseURI "http://example.org")
                  (u/strip-service-attrs))
          updated (-> (merge current new)
                      (u/update-timestamps)
                      (merge resource-links)
                      (c/set-operations request)
                      (c/validate))
          stripped (apply dissoc updated stripped-keys)]
      (db/edit stripped))))

(defmethod crud/edit resource-name
           [request]
  (edit-impl request))

;;
;; CloudEntryPoint doesn't follow the usual /cimi/ResourceName/UUID
;; pattern, so the routes must be defined explicitly.
;;
(defroutes routes
           (GET base-uri request
                (crud/retrieve (assoc-in request [:params :resource-name] resource-name)))
           (PUT base-uri request
                (crud/edit (assoc-in request [:params :resource-name] resource-name)))
           (ANY base-uri request
                (u/bad-method request)))
