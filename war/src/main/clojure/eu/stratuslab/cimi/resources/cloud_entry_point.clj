(ns eu.stratuslab.cimi.resources.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :as set]
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.resources.machine-configuration :as mc]
    [eu.stratuslab.cimi.resources.job :as job]

    [eu.stratuslab.cimi.resources.volume :as volume]
    [eu.stratuslab.cimi.resources.volume-template :as volume-template]
    [eu.stratuslab.cimi.resources.volume-configuration :as volume-configuration]
    [eu.stratuslab.cimi.resources.volume-image :as volume-image]

    [clojure.tools.logging :refer [debug info warn]]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [ring.util.response :as rresp]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.data.json :as json]

    [cemerick.friend :as friend])
  (:import [java.io InputStreamReader]))

(def ^:const resource-type "CloudEntryPoint")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const base-uri "/")

;; FIXME: Generate these automatically.
(def resource-links
  {:machineConfigs {:href mc/resource-type}
   :jobs {:href job/resource-type}
   :volumes {:href volume/resource-type}
   :volumeTemplates {:href volume-template/resource-type}
   :volumeConfigs {:href volume-configuration/resource-type}
   :volumeImages {:href volume-image/resource-type}})

(def-map-schema CloudEntryPoint
                common/CommonAttrs
                [[:baseURI] NonEmptyString
                 (optional-path [:resourceMetadata]) common/ResourceLink
                 (optional-path [:systems]) common/ResourceLink
                 (optional-path [:systemTemplates]) common/ResourceLink
                 (optional-path [:machines]) common/ResourceLink
                 (optional-path [:machineTemplates]) common/ResourceLink
                 (optional-path [:machineConfigs]) common/ResourceLink
                 (optional-path [:machineImages]) common/ResourceLink
                 (optional-path [:credentials]) common/ResourceLink
                 (optional-path [:credentialTemplates]) common/ResourceLink
                 (optional-path [:volumes]) common/ResourceLink
                 (optional-path [:volumeTemplates]) common/ResourceLink
                 (optional-path [:volumeConfigs]) common/ResourceLink
                 (optional-path [:volumeImages]) common/ResourceLink
                 (optional-path [:networks]) common/ResourceLink
                 (optional-path [:networkTemplates]) common/ResourceLink
                 (optional-path [:networkConfigs]) common/ResourceLink
                 (optional-path [:networkPorts]) common/ResourceLink
                 (optional-path [:networkPortTemplates]) common/ResourceLink
                 (optional-path [:networkPortConfigs]) common/ResourceLink
                 (optional-path [:addresses]) common/ResourceLink
                 (optional-path [:addressTemplates]) common/ResourceLink
                 (optional-path [:forwardingGroups]) common/ResourceLink
                 (optional-path [:forwardingGroupTemplates]) common/ResourceLink
                 (optional-path [:jobs]) common/ResourceLink
                 (optional-path [:meters]) common/ResourceLink
                 (optional-path [:meterTemplates]) common/ResourceLink
                 (optional-path [:meterConfigs]) common/ResourceLink
                 (optional-path [:eventLogs]) common/ResourceLink
                 (optional-path [:eventLogTemplates]) common/ResourceLink])

(def validate (utils/create-validation-fn CloudEntryPoint))

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (if (friend/authorized? #{:eu.stratuslab.cimi.authn/admin} friend/*identity*)
    (let [ops [{:rel (:edit common/action-uri) :href base-uri}]]
      (assoc resource :operations ops))
    resource))

(defn add
  "Creates a minimal CloudEntryPoint in the database.  Note that
   only the common attributes are saved in the database; links to
   resource types are generated when the service starts.

   NOTE: Unlike other resources, the :id is 'CloudEntryPoint'
   rather than the relative URI for the resource."
  [cb-client]

  (let [record (-> {:id resource-type
                    :resourceURI type-uri}
                   (utils/set-time-attributes))]
    (cbc/add-json cb-client resource-type record {:observe true
                                                  :persist :master
                                                  :replicate :zero})))

(defn retrieve
  "Returns the data associated with the CloudEntryPoint.  This combines
   the values of the common attributes in the database with the baseURI
   from the web container and the generated resource links."
  [cb-client baseURI]
  (if-let [cep (cbc/get-json cb-client resource-type)]
    (rresp/response (-> cep
                        (assoc :baseURI baseURI)
                        (merge resource-links)
                        (add-rops)))
    (rresp/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [cb-client baseURI entry]
  (if-let [current (cbc/get-json cb-client resource-type)]
    (let [db-doc (-> entry
                     (select-keys [:name :description :properties])
                     (merge current)
                     (utils/set-time-attributes))
          doc (-> db-doc
                  (assoc :baseURI baseURI)
                  (merge resource-links)
                  (add-rops)
                  (validate))]
      (if (cbc/set-json cb-client resource-type db-doc)
        (rresp/response doc)
        (rresp/status (rresp/response nil) 409)))
    (rresp/not-found nil)))

(defroutes resource-routes
           (GET base-uri {:keys [cb-client base-uri] :as request}
                (retrieve cb-client base-uri))
           (PUT base-uri {:keys [cb-client base-uri body] :as request}
                (println request)
                (friend/authorize #{:eu.stratuslab.cimi.authn/admin}
                                  (let [json (utils/body->json body)]
                                    (edit cb-client base-uri json)))))
