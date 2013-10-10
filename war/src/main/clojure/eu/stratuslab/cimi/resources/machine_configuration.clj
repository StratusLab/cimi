(ns eu.stratuslab.cimi.resources.machine-configuration
  "Utilities for managing the CRUD features for machine configurations."
  (:require
    [clojure.string :as str]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
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

(def ^:const resource-type "MachineConfiguration")

(def ^:const collection-resource-type "MachineConfigurationCollection")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://schemas.dmtf.org/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/" resource-type))

(def-map-schema Disk
                [[:capacity] PosIntegral
                 [:format] NonEmptyString
                 (optional-path [:initialLocation]) NonEmptyString])

(def-seq-schema Disks
                (constraints (fn [s] (pos? (count s))))
                [Disk])

(def-map-schema MachineConfiguration
                common/CommonAttrs
                [[:cpu] PosIntegral
                 [:memory] PosIntegral
                 [:cpuArch] #{"68000" "Alpha" "ARM" "Itanium" "MIPS" "PA_RISC"
                              "POWER" "PowerPC" "x86" "x86_64" "zArchitecture", "SPARC"}
                 (optional-path [:disks]) Disks])

(def validate (utils/create-validation-fn MachineConfiguration))

(defn uuid->uri
  "Convert a uuid into the URI for a MachineConfiguration resource.
   The URI must not have a leading slash."
  [uuid]
  (str resource-type "/" uuid))

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
  "Add a new MachineConfiguration to the database.  The entry contains
   the fields for the new MachineConfiguration.  The :id, :resourceURI,
   :created, and :updated fields will be provided automatically.  The
   entry will be validated before being added to the database."
  ([cb-client] (add cb-client {}))

  ([cb-client entry]
   (let [uri (uuid->uri (utils/create-uuid))
         entry (-> entry
                   (utils/strip-service-attrs)
                   (assoc :id uri)
                   (assoc :resourceURI type-uri)
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
                         (add-rops)
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

        configs (->> (cbc/query cb-client v q)
                     (map cbc/view-doc-json)
                     (map add-rops))
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count configs)})]
    (rresp/response (if (empty? configs)
                      collection
                      (assoc collection :machineConfigurations configs)))))

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