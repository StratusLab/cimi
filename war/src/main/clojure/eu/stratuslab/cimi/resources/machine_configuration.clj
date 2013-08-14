(ns eu.stratuslab.cimi.resources.machine-configuration
  "Utilities for managing the CRUD features for machine configurations."
  (:require 
    [clojure.string :as str]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.cb.bootstrap :as bootstrap]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [ring.util.response :as rresp]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    [clojure.tools.logging :refer [debug info error]]))

(def ^:const resource-type "MachineConfiguration")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

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

(defn uuid->uri
  "Convert a uuid into the URI for a MachineConfiguration resource."
  [uuid]
  (str base-uri "/" uuid))

(defn validate 
  "Validates the MachineConfiguration entry against the defined schema.
   This method will return the entry itself if valid; it will raise an
   exception otherwise."
  [entry]
  (let [errors (validation-errors MachineConfiguration entry)]
    (if (empty? errors)
      entry
      (throw (Exception. (str "resource does not satisfy defined schema\n"
                           (str/join "\n" errors)))))))

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
    (rresp/response json)
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
        v (cbc/get-view cb-client bootstrap/design-doc-name "resource-uri")
        results (cbc/query cb-client v q)]
    (doall (map cbc/view-doc-json results))))

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