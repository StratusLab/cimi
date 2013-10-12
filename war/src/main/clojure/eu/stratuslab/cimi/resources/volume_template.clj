(ns eu.stratuslab.cimi.resources.volume-template
  "Template combines the characteristics of the Volume to be created
   through the referenced (or embedded) VolumeConfiguration and the source
   for initializing the Volume through the referenced (or embedded)
   VolumeImage."
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

(def ^:const resource-type "VolumeTemplate")

(def ^:const collection-resource-type "VolumeTemplateCollection")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://schemas.dmtf.org/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/" resource-type))

(def validate (u/create-validation-fn schema/VolumeTemplate))

(defn uuid->uri
  "Convert a uuid into the URI for a VolumeTemplate resource.
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

(defn add
  "Add a new VolumeConfiguration to the database."
  [cb-client entry]
  (let [uri (uuid->uri (u/create-uuid))
        entry (-> entry
                  (u/strip-service-attrs)
                  (assoc :id uri)
                  (assoc :resourceURI type-uri)
                  (u/set-time-attributes)
                  (validate))]
    (if (cbc/add-json cb-client uri entry)
      (r/created uri)
      (r/status (r/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the data associated with the requested VolumeConfiguration
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
  "Deletes the VolumeTemplate."
  [cb-client uuid]
  (if (cbc/delete cb-client (uuid->uri uuid))
    (r/response nil)
    (r/not-found nil)))

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

        volume-templates (->> (cbc/query cb-client v q)
                              (map cbc/view-doc-json)
                              (map add-rops))
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count volume-templates)})]
    (r/response (if (empty? volume-templates)
                  collection
                  (assoc collection :volumeTemplates volume-templates)))))

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

