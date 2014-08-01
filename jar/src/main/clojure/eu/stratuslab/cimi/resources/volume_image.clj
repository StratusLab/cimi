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
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.impl.schema :as schema]
    [eu.stratuslab.cimi.resources.volume :as v]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :volumeImages)

(def ^:const resource-type "VolumeImage")

(def ^:const collection-resource-type "VolumeImageCollection")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://schemas.dmtf.org/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/cimi/" resource-type))

(def collection-acl {:owner {:principal "::ADMIN" :type "ROLE"}
                     :rules [{:principal "::USER" :type "ROLE" :right "MODIFY"}]})

(def validate (u/create-validation-fn v/VolumeImage))

(defn uuid->uri
  "Convert the uuid into a resource URI.  NOTE: unlike for other resources,
   the UUID is the base64-encoded, SHA-1 checksum of the image."
  [uuid]
  (str resource-type "/" uuid))

(defn image-id
  "If the :initialLocation/:href value is a valid image identifier
   (base64-encoded, SHA-1 checksum), then return the value.  Return
   nil otherwise."
  [{:keys [imageLocation] :or {imageLocation {}}}]
  (if-let [href (:href imageLocation)]
    (if (re-matches #"^[\w-]{27}$" href)
      href)))

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
  (let [href (:id resource)
        ops [{:rel (:edit schema/action-uri) :href href}
             {:rel (:delete schema/action-uri) :href href}]]
    (assoc resource :operations ops)))

(defn add
  "Adds a new VolumeImage to the database.  This will kick off a
   job that will attempt to download and cache the given image.
   The initialLocation attribute should be set to the Marketplace
   identifier for Marketplace images; raw http(s) URLs may be used
   to bring in images from elsewhere."
  [cb-client entry]
  (let [uuid (or (image-id entry) (u/random-uuid))
        uri (uuid->uri uuid)
        entry (-> entry
                  (u/strip-service-attrs)
                  (assoc :id uri)
                  (assoc :resourceURI type-uri)
                  (u/update-timestamps)
                  (assoc :state "CREATING")
                  (validate))]
    (if (cbc/add-json cb-client uri entry)
      (-> (job/launch cb-client uri "create")
          (r/status 201)
          (r/header "Location" uri))
      (r/status (r/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the data associated with the requested VolumeImage
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
                         (u/update-timestamps)
                         (add-rops)
                         (validate))]
        (if (cbc/set-json cb-client uri updated)
          (r/response updated)
          (r/status (r/response nil) 409))) ;; conflict
      (r/not-found nil))))

(defn delete
  "Submits an asynchronous request to delete the VolumeImage.
   The job is responsible for deleting the VolumeImage resource
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

        volume-images (->> (cbc/query cb-client v q)
                           (map cbc/view-doc-json)
                           (map add-rops))
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count volume-images)})]
    (r/response (if (empty? volume-images)
                  collection
                  (assoc collection :volumeImages volume-images)))))

#_(defroutes collection-routes
           (POST base-uri {:keys [cb-client body]}
                 (if (a/can-modify? collection-acl)
                   (let [json (u/body->json body)]
                     (add cb-client json))
                   (u/unauthorized)))
           (GET base-uri {:keys [cb-client body]}
                (if (a/can-modify? collection-acl)
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
