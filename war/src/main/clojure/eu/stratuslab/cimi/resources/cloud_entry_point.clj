(ns eu.stratuslab.cimi.resources.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require 
    [clojure.tools.logging :as log]
    [clojure.set :as set]
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [clojure.tools.logging :refer [debug info warn]]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [ring.util.response :as rresp]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.data.json :as json])
  (:import [java.io InputStreamReader]))

(def ^:const resource-type "CloudEntryPoint")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const base-uri "/")

(def-map-schema ResourceLink
  [[:href] NonEmptyString])

(def-map-schema CloudEntryPoint
  common/CommonAttrs
  [[:baseURI] NonEmptyString
   (optional-path [:resourceMetadata]) ResourceLink
   (optional-path [:systems]) ResourceLink
   (optional-path [:systemTemplates]) ResourceLink
   (optional-path [:machines]) ResourceLink
   (optional-path [:machineTemplates]) ResourceLink
   (optional-path [:machineConfigs]) ResourceLink
   (optional-path [:machineImages]) ResourceLink
   (optional-path [:credentials]) ResourceLink
   (optional-path [:credentialTemplates]) ResourceLink
   (optional-path [:volumes]) ResourceLink
   (optional-path [:volumeTemplates]) ResourceLink
   (optional-path [:volumeConfigs]) ResourceLink
   (optional-path [:volumeImages]) ResourceLink
   (optional-path [:networks]) ResourceLink
   (optional-path [:networkTemplates]) ResourceLink
   (optional-path [:networkConfigs]) ResourceLink
   (optional-path [:networkPorts]) ResourceLink
   (optional-path [:networkPortTemplates]) ResourceLink
   (optional-path [:networkPortConfigs]) ResourceLink
   (optional-path [:addresses]) ResourceLink
   (optional-path [:addressTemplates]) ResourceLink
   (optional-path [:forwardingGroups]) ResourceLink
   (optional-path [:forwardingGroupTemplates]) ResourceLink
   (optional-path [:jobs]) ResourceLink
   (optional-path [:meters]) ResourceLink
   (optional-path [:meterTemplates]) ResourceLink
   (optional-path [:meterConfigs]) ResourceLink
   (optional-path [:eventLogs]) ResourceLink
   (optional-path [:eventLogTemplates]) ResourceLink])

(def validate (utils/create-validation-fn CloudEntryPoint))

(defn add
  "Creates a new CloudEntryPoint from the given data.  This normally only occurs
   during the service bootstrap process when the database has not yet been 
   initialized."
  [cb-client]
  
  (let [record (->> {:id base-uri
                     :name resource-type
                     :description "StratusLab Cloud"
                     :resourceURI type-uri}
                 (utils/set-time-attributes))]
    (cbc/add-json cb-client base-uri record {:observe true
                                             :persist :master
                                             :replicate :zero})))

(defn retrieve
  "Returns the data associated with the CloudEntryPoint.  There is
  exactly one such entry in the database.  The identifier is the root
  resource name '/'.  The baseURI must be passed as this is taken from 
  the ring request."
  [cb-client baseURI]
  (if-let [json (cbc/get-json cb-client base-uri)]
    (rresp/response (assoc json :baseURI baseURI))
    (rresp/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [cb-client baseURI entry]
  (if-let [current (cbc/get-json cb-client base-uri)]
    (let [updated (->> entry
                   (utils/strip-service-attrs)
                   (merge current)
                   (utils/set-time-attributes))
          updated (-> updated
                    (assoc :baseURI baseURI)
                    (validate))]
      (if (cbc/set-json cb-client base-uri updated)
        (rresp/response updated)
        (rresp/status (rresp/response nil) 409)))
    (rresp/not-found nil)))

(defroutes resource-routes
  (GET base-uri {:keys [cb-client base-uri]}
    (retrieve cb-client base-uri))
  (PUT base-uri {:keys [cb-client base-uri body]}
    (let [json (utils/body->json body)]
      (edit cb-client base-uri json))))
