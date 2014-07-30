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

(ns eu.stratuslab.cimi.resources.machine-image
  "Utilities for managing the CRUD features for machine images."
  (:require
    [clojure.string :as str]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [clojure.data.json :as json]
    [eu.stratuslab.cimi.resources.impl.schema :as schema]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [cemerick.friend :as friend]
    [schema.core :as s]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :machineImages)

(def ^:const resource-type "MachineImage")

(def ^:const collection-resource-type "MachineImageCollection")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://schemas.dmtf.org/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/cimi/" resource-type))

(def collection-acl {:owner {:principal "::ADMIN" :type "ROLE"}
                     :rules [{:principal "::USER" :type "ROLE" :right "MODIFY"}]})


(defn uuid->uri
  [uuid]
  (str resource-type "/" uuid))

;;
;; MachineImage schema
;;

(def machine-image-state-values (s/enum "CREATING" "AVAILABLE" "DELETING" "ERROR"))

(def machine-image-type-values (s/enum "IMAGE" "SNAPSHOT" "PARTIAL_SNAPSHOT"))

(def MachineImage
  (merge c/CommonAttrs
         c/AclAttr
         {:state                          machine-image-state-values
          :type                           machine-image-type-values
          (s/optional-key :imageLocation) c/NonBlankString
          (s/optional-key :relatedImage)  c/ResourceLink}))

(def validate (u/create-validation-fn MachineImage))

(defn add-cops
  "Adds the collection operations to the given resource."
  [resource]
  (if (a/can-modify? collection-acl)
    (let [ops [{:rel (:add schema/action-uri) :href base-uri}]]
      (assoc resource :operations ops))
    resource))

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (if (a/can-modify? (:acl resource))
    (let [href (:id resource)
          ops [{:rel (:edit schema/action-uri) :href href}
               {:rel (:delete schema/action-uri) :href href}]]
      (assoc resource :operations ops))
    resource))

(defn add
  "Add a new MachineImage to the database.  The entry contains
   the fields for the new MachineImage.  The :id, :resourceURI,
   :created, and :updated fields will be provided automatically.  The
   entry will be validated before being added to the database."
  ([cb-client] (add cb-client {}))

  ([cb-client entry]
   (let [uri (uuid->uri (u/random-uuid))
         entry (-> entry
                   (u/strip-service-attrs)
                   (assoc :id uri)
                   (assoc :resourceURI type-uri)
                   (u/set-time-attributes)
                   (a/add-acl (friend/current-authentication))
                   (validate))]
     (if (cbc/add-json cb-client uri entry)
       (r/created uri)
       (r/status (r/response (str "cannot create " uri)) 400)))))

(defn retrieve
  "Returns the data associated with the requested MachineImage
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (if (a/can-view? (friend/current-authentication) (:acl json))
      (r/response (add-rops json))
      (u/unauthorized))
    (r/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (friend/current-authentication) (:acl current))
        (let [updated (->> entry
                           (u/strip-service-attrs)
                           (merge current)
                           (u/set-time-attributes)
                           (add-rops)
                           (validate))]
          (if (cbc/set-json cb-client uri updated)
            (r/response updated)
            (r/status (r/response nil) 409))) ;; conflict
        (u/unauthorized))
      (r/not-found nil))))

(defn delete
  "Deletes the named machine image."
  [cb-client uuid]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (friend/current-authentication) (:acl current))
        (if (cbc/delete cb-client uri)
          (r/response nil)
          (r/not-found nil))
        (u/unauthorized))
      (r/not-found nil))))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [principals (a/authn->principals (friend/current-authentication))
        configs (u/viewable-resources cb-client resource-type principals opts)
        configs (map add-rops configs)
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count configs)})]
    (r/response (if (empty? collection)
                  collection
                  (assoc collection :machineImages configs)))))

#_(defroutes collection-routes
           (POST base-uri {:keys [cb-client body]}
                 (if (a/can-modify? collection-acl)
                   (let [json (u/body->json body)]
                     (add cb-client json))
                   (u/unauthorized)))
           (GET base-uri {:keys [cb-client body]}
                (if (a/can-view? collection-acl)
                  (let [json (u/body->json body)]
                    (query cb-client json))
                  (u/unauthorized)))
           (ANY base-uri []
                (u/bad-method)))

#_(def resource-routes
  (let-routes [uri (str base-uri "/:uuid")]
              (GET uri [uuid :as {cb-client :cb-client}]
                   (retrieve cb-client uuid))
              (PUT uri [uuid :as {cb-client :cb-client body :body}]
                   (let [json (u/body->json body)]
                     (edit cb-client uuid json)))
              (DELETE uri [uuid :as {cb-client :cb-client}]
                      (delete cb-client uuid))
              (ANY uri []
                   (u/bad-method))))

#_(defroutes routes
           collection-routes
           resource-routes)
