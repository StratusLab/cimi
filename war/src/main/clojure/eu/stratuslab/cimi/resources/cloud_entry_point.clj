(ns eu.stratuslab.cimi.resources.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require 
    [clojure.set :as set]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.middleware.cfg-params :as cfg]
    [clojure.tools.logging :refer [debug info warn]]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [com.ashafa.clutch :as clutch]))

(def ^:const resource-type "CloudEntryPoint")

(def ^:const resource-uri "http://www.dmtf.org/cimi/CloudEntryPoint")

(def ^:const resource-base-url "/")

(def cep-attributes
  "These are the attributes specific to a CloudEntryPoint."
  #{:baseURI :resourceMetadata
    :systems :systemTemplates
    :machines :machineTemplates :machineConfigs :machineImages
    :credentials :credentialTemplates
    :volumes :volumeTemplates :volumeConfigs :volumeImages
    :networks :networkTemplates :networkConfigs
    :networkPorts :networkPortTemplates :networkPortConfigs
    :addresses :addressTemplates
    :forwardingGroups :forwardingGroupTemplates
    :jobs
    :meter :meterTemplates :meterConfigs
    :eventLogs :eventLogTemplates})

(def attributes
  "These are the attributes allowed for a CloudEntryPoint."
  (set/union common/attributes cep-attributes))

(def immutable-attributes
  "These are the attributes for a CloudEntryPoint that cannot
   be modified."
  (set/union common/immutable-attributes cep-attributes))

(defn strip-unknown-attributes [m]
  (select-keys m attributes))

(defn strip-immutable-attributes [m]
  (let [ks (set/difference (set (keys m)) immutable-attributes)]
    (select-keys m ks)))

(defn create
  "Creates a new CloudEntryPoint from the given data.  This normally only occurs
   during the service bootstrap process when the database has not yet been 
   initialized."
  [db-url]
  
  (let [record (->> 
                 {:id resource-base-url
                  :name resource-type
                  :description "StratusLab Cloud"
                  :resource-type resource-type
                  :resourceURI resource-uri}
                 (utils/set-time-attributes)
                 (utils/set-db-id resource-base-url))]
    (clutch/put-document db-url record)))

(defn retrieve
  "Returns the data associated with the CloudEntryPoint.  There is
  exactly one such entry in the database.  The identifier is the root
  resource name '/'.  The baseURI must be passed as this is taken from 
  the ring request."
  [req]
  (let [baseURI (:base-uri req)
        db-url (cfg/db-url req)
        doc (clutch/get-document db-url resource-base-url)]
    (assoc doc :baseURI (:baseURI req))))

(defn update
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [req]
  (let [db-url (cfg/db-url req)
        update (->> req
                 (strip-unknown-attributes)
                 (strip-immutable-attributes)
                 (utils/set-time-attributes)
                 (utils/set-db-id resource-base-url))
        current (clutch/get-document db-url resource-base-url)
        newdoc (merge current update)]
    (clutch/put-document db-url newdoc)))

(defroutes resource-routes
  (GET "/" {:as req} {:body (retrieve req)})
  (PUT "/" {:as req} (update req) {}))
