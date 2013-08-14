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
  [req]
  (let [baseURI (:base-uri req)
        cb-client (:cb-client req)
        doc (cbc/get-json cb-client base-uri)]
    (assoc doc :baseURI baseURI)))

(defn edit
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [req]
  (let [cb-client (:cb-client req)
        body (InputStreamReader. (:body req))
        json (json/read body :key-fn keyword)
        update (->> json
                 (utils/strip-service-attrs)
                 (utils/set-time-attributes))
        current (cbc/get-json cb-client base-uri)
        newdoc (merge current update)]
    (log/info "json:" json)
    (log/info "update:" update)
    (log/info "updating CloudEntryPoint:" newdoc)
    (cbc/set-json cb-client base-uri newdoc)))

(defroutes resource-routes
  (GET base-uri {:as req} {:body (retrieve req)})
  (PUT base-uri {:as req} (edit req) {}))
