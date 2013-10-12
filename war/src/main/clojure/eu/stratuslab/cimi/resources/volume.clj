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
    [eu.stratuslab.cimi.resources.schema :as schema]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]))

(def ^:const resource-type "Volume")

(def ^:const collection-resource-type "VolumeCollection")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://schemas.dmtf.org/cimi/1/" collection-resource-type))

(def ^:const create-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type "Create"))

(def ^:const base-uri (str "/" resource-type))

(def validate (u/create-validation-fn schema/Volume))

(def validate-create (u/create-validation-fn schema/VolumeCreate))

(defn uuid->uri
  "Convert a uuid into the URI for a MachineConfiguration resource.
   The URI must not have a leading slash."
  [uuid]
  (str resource-type "/" uuid))

(defn add-cops
  "Adds the collection operations to the given resource."
  [resource]
  (let [ops [{:rel (:add schema/action-uri) :href base-uri}]]
    (assoc resource :operations ops)))

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (let [href (:id resource)
        ops [{:rel (:edit schema/action-uri) :href href}
             {:rel (:delete schema/action-uri) :href href}]]
    (assoc resource :operations ops)))

(defn volume-skeleton [uri entry]
  (if (u/correct-resource? create-uri entry)
    (-> entry
        (u/strip-service-attrs)
        (dissoc :volumeTemplate)
        (assoc :resourceURI type-uri)
        (u/set-time-attributes)
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
      (validate)))

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
  (let [uri (uuid->uri (u/create-uuid))
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

(defn retrieve
  "Returns the data associated with the requested Volume
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (r/response (add-rops json))
    (r/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will 
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (let [updated (->> entry
                         (u/strip-service-attrs)
                         (merge current)
                         (u/set-time-attributes)
                         (add-rops)
                         (validate))]
        (if (cbc/set-json cb-client uri updated)
          (r/response updated)
          (r/status (r/response nil) 409))) ;; conflict
      (r/not-found nil))))

(defn delete
  "Submits an asynchronous request to delete the volume.
   The job is responsible for deleting the Volume resource
   if the delete request is successful.  The response will
   always return an accepted (202) code."
  [cb-client uuid]
  (job/launch cb-client (uuid->uri uuid) "delete"))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [q (cbq/create-query (merge {:include-docs true
                                    :key type-uri
                                    :limit 100
                                    :stale false
                                    :on-error :continue}
                                   opts))
        v (views/get-view cb-client :resource-uri)

        volumes (->> (cbc/query cb-client v q)
                     (map cbc/view-doc-json)
                     (map add-rops))
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count volumes)})]
    (r/response (if (empty? volumes)
                  collection
                  (assoc collection :volumes volumes)))))

(defroutes collection-routes
           (POST base-uri {:keys [cb-client body]}
                 (let [json (u/body->json body)]
                   (add cb-client json)))
           (GET base-uri {:keys [cb-client body]}
                (let [json (u/body->json body)]
                  (query cb-client json)))
           (ANY base-uri []
                (u/bad-method)))

(def resource-routes
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

(defroutes routes
           collection-routes
           resource-routes)
