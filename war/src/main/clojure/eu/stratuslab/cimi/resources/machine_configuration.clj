(ns eu.stratuslab.cimi.resources.machine-configuration
  "Utilities for managing the CRUD features for machine configurations."
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [ring.util.response :as rresp]
    [clojure.tools.logging :refer [debug info error]]))

(def ^:const resource-type "MachineConfiguration")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const base-uri (str "/" resource-type))

(defn uuid->uri
  "Convert a uuid into the URI for a MachineConfiguration resource."
  [uuid]
  (str base-uri "/" uuid))

;; TODO: This function must actually validate the entry!
(defn validate 
  "Validates the MachineConfiguration entry against the defined schema.
   This method will return the entry itself if valid; it will raise an
   exception otherwise."
  [entry] entry)

(defn add
  "Add a new MachineConfiguration to the database.  The entry contains
   the fields for the new MachineConfiguration.  The :id, :resourceURI,
   :created, and :updated fields will be provided automatically.  The
   entry will be validated before being added to the database."
  ([cb-client] (add cb-client {}))

  ([cb-client entry]
    (let [uri (uuid->uri (utils/create-uuid))
          entry (-> entry
                  (assoc :id uri)
                  (assoc :resourceURI type-uri)
                  (dissoc :created)  ;; ensure not set by user
                  (utils/set-time-attributes)
                  (validate))]
      (if (cbc/add-json cb-client uri entry)
        (rresp/created uri)
        (rresp/status (rresp/response (str "cannot create " uri)) 400)))))

(defn retrieve
  "Returns the data associated with the requested MachineConfiguration
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (rresp/response json)
    (rresp/not-found nil)))

(defn strip-service-attrs
  "Strips keys from the map that are controlled by the service itself.
   For example, the :created and :updated keys."
  [m]
  (dissoc m :id :created :updated :resourceURI))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will 
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (let [updated (->> entry
                      (strip-service-attrs)
                      (merge current)
                      (utils/set-time-attributes)
                      (validate))]
        (if (cbc/set-json cb-client uri updated)
          (rresp/response updated)
          (rresp/status (rresp/response nil) 409))) ;; conflict
      (rresp/not-found nil))))

(defn delete
  "Deletes the named machine configuration."
  [cb-client uuid]
  (if (cbc/delete cb-client (uuid->uri uuid))
    (rresp/response nil)
    (rresp/not-found nil)))

;; TODO: Create a real implementation!
(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & options]
  {})

(defn body->json [body]
  (json/read (io/reader body) :key-fn keyword))

(defroutes resource-routes
  (POST base-uri {:keys [cb-client body]}
    (add cb-client body))
  (GET base-uri {:keys [cb-client body]}
    (query cb-client body))
  (GET (str base-uri "/:uuid") [uuid :as {cb-client :cb-client}]
    (retrieve cb-client uuid))
  (PUT (str base-uri "/:uuid") [uuid :as {cb-client :cb-client body :body}]
    (let [json (body->json body)]
      (edit cb-client uuid json)))
  (DELETE (str base-uri "/:uuid") [uuid :as {cb-client :cb-client}]
    (delete cb-client uuid)))