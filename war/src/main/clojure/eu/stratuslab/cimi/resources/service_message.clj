(ns eu.stratuslab.cimi.resources.service-message
  "Allows administrators to provide general service messages for users.
   A short title is provided in the 'name' attribute and the 'description'
   attribute holds the full message.  It is recommended that the message
   be formatted in Markdown format.

   NOTE: Unlike for most other resources, the unique identifier for the
   message is the 'created' timestamp in UTC.  This allows the query to 
   return messages in reversed time order."
  (:require 
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.volume-configuration :refer [VolumeConfigurationAttrs]]
    [eu.stratuslab.cimi.resources.volume-image :refer [VolumeImageAttrs]]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [ring.util.response :as rresp]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.tools.logging :as log]))

(def ^:const resource-type "ServiceMessage")

(def ^:const collection-resource-type "ServiceMessageCollection")

(def ^:const type-uri (str "http://stratuslab.eu/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://stratuslab.eu/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/" resource-type))

(def-map-schema ServiceMessage
  common/CommonAttrs
  [[:name] NonEmptyString
   [:description] NonEmptyString])

(def validate (utils/create-validation-fn ServiceMessage))

(defn uuid->uri
  "Convert a uuid into the URI for a ServiceMessage resource.
   The URI must not have a leading slash.  The unique identifer
   for these resources is the 'created' timestamp in UTC."
  [uuid]
  (str resource-type "/" uuid))

(defn add-id
  "Creates the entry's identifier by taking creating the uri from
   the created time and returning the entry with the :id attribute
   added."
  [entry]
  (->> (:created entry)
    (uuid->uri)
    (assoc entry :id)))

(defn add-cops
  "Adds the collection operations to the given resource."
  [resource]
  (let [ops [{:rel (:add common/action-uri) :href base-uri}]]
    (assoc resource :operations ops)))

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (let [href (:id resource)
        ops [{:rel (:edit common/action-uri) :href href}
             {:rel (:delete common/action-uri) :href href}]]
    (assoc resource :operations ops)))

(defn add
  "Add a new ServiceMessage to the database."
  [cb-client entry]
  (let [entry (-> entry
                (utils/strip-service-attrs)
                (assoc :resourceURI type-uri)
                (utils/set-time-attributes)
                (add-id)
                (validate))
        uri (:id entry)]
    (if (cbc/add-json cb-client uri entry)
      (rresp/created uri)
      (rresp/status (rresp/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the data associated with the requested ServiceMessage
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (rresp/response (add-rops json))
    (rresp/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will 
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (let [updated (->> entry
                      (utils/strip-service-attrs)
                      (merge current)
                      (utils/set-time-attributes)
                      (validate))]
        (if (cbc/set-json cb-client uri updated)
          (rresp/response updated)
          (rresp/status (rresp/response nil) 409))) ;; conflict
      (rresp/not-found nil))))

(defn delete
  "Deletes the ServiceMessage."
  [cb-client uuid]
  (if (cbc/delete cb-client (uuid->uri uuid))
    (rresp/response nil)
    (rresp/not-found nil)))

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
        
        messages (->> (cbc/query cb-client v q)
                   (map cbc/view-doc-json)
                   (map add-rops))
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count messages)})]
    (rresp/response (if (empty? messages)
                      collection
                      (assoc collection :serviceMessages messages)))))

(defroutes resource-routes
  (POST base-uri {:keys [cb-client body]}
    (let [json (utils/body->json body)]
      (add cb-client json)))
  (GET base-uri {:keys [cb-client body]}
    (let [json (utils/body->json body)]
      (query cb-client json)))
  (GET (str base-uri "/:uuid") [uuid :as {cb-client :cb-client}]
    (retrieve cb-client uuid))
  (PUT (str base-uri "/:uuid") [uuid :as {cb-client :cb-client body :body}]
    (let [json (utils/body->json body)]
      (edit cb-client uuid json)))
  (DELETE (str base-uri "/:uuid") [uuid :as {cb-client :cb-client}]
    (delete cb-client uuid)))
