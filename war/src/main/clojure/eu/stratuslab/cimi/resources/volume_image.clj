(ns eu.stratuslab.cimi.resources.volume-image
  "A CIMI VolumeImage corresponds to the images in the StratusLab Marketplace.
   These are images that can be used to initialize volumes within the cloud
   infrastructure (either machine or data images).

   NOTE: Unlike for other resources, the unique identifier (UUID) for
   VolumeImages are the base64-encoded SHA-1 hash of the image."
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

(def ^:const resource-type "VolumeImage")

(def ^:const collection-resource-type "VolumeImageCollection")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://schemas.dmtf.org/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/" resource-type))

(def validate (u/create-validation-fn schema/VolumeImage))

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
  (let [ops [{:rel (:add schema/action-uri) :href base-uri}]]
    (assoc resource :operations ops)))

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
  (let [uuid (or (image-id entry) (u/create-uuid))
        uri (uuid->uri uuid)
        entry (-> entry
                  (u/strip-service-attrs)
                  (assoc :id uri)
                  (assoc :resourceURI type-uri)
                  (u/set-time-attributes)
                  (assoc :state "CREATING")
                  (validate))]
    (if (cbc/add-json cb-client uri entry)
      (let [job-uri (job/add cb-client {:targetResource uri
                                        :action "create"})]
        (r/header (r/created uri) "CIMI-Job-URI" job-uri))
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
                         (u/set-time-attributes)
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