(ns eu.stratuslab.cimi.resources.machine-configurations
  "Utilities for managing the CRUD features for machine configurations."
  (:require 
    [clojure.set :as set]
    [eu.stratuslab.cimi.resources.common :as common]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.db-utils :as db-utils]
    [eu.stratuslab.cimi.middleware.cfg-params :as cfg]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [compojure.response :as response]
    [clojure.tools.logging :refer [debug info error]]))

(def ^:const resource-type "MachineConfiguration")

(def resource-uri (str common/cimi-uri resource-type))

(def ^:const resource-base-url "/")

(def mc-attributes
  "These are the attributes specific to a MachineConfiguration."
  #{:cpu :memory :disks :cpuArch})

(def attributes
  "These are the attributes allowed for a MachineConfiguration."
  (set/union common/attributes mc-attributes))

(def immutable-attributes
  "These are the attributes for a MachineConfiguration that cannot
   be modified."
  (set/union common/immutable-attributes mc-attributes))

(defn strip-unknown-attributes [m]
  (select-keys m attributes))

(defn strip-immutable-attributes [m]
  (let [ks (set/difference (set (keys m)) immutable-attributes)]
    (select-keys m ks)))

(defn create
  "Creates a new MachineConfiguration from the given data."
  [db-cfg]
  
  (let [record (->> 
                 {:id resource-base-url
                  :name resource-type
                  :description "StratusLab Cloud"
                  :resource-type resource-type
                  :resourceURI resource-uri}
                 (utils/set-time-attributes))]
    (db-utils/create db-cfg resource-base-url record)))

(defn retrieve
  "Returns the data associated with the CloudEntryPoint.  There is
  exactly one such entry in the database.  The identifier is the root
  resource name '/'.  The baseURI must be passed as this is taken from 
  the ring request."
  [req]
  (let [baseURI (:base-uri req)
        db-cfg (cfg/db-cfg req)
        doc (db-utils/retrieve db-cfg resource-base-url)]
    (assoc doc :baseURI (:baseURI req))))

(defn update
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [req]
  (let [db-cfg (cfg/db-cfg req)
        update (->> req
                 (strip-unknown-attributes)
                 (strip-immutable-attributes)
                 (utils/set-time-attributes))
        current (db-utils/retrieve db-cfg resource-base-url)
        newdoc (merge current update)]
    (db-utils/update db-cfg resource-base-url newdoc)))

(defn delete
  "Deletes the named machine configuration."
  [req]
  (let [id "dummy"
        db-cfg (cfg/db-cfg req)
        resource-uri (str resource-base-url "/" id)]
    (db-utils/delete db-cfg resource-base-url)))

(defroutes resource-routes
  (POST "/MachineConfiguration" {:as req} {:body (create req)})
  (GET "/MachineConfiguration" {:as req} {:body (list req)})
  (GET "/MachineConfiguration/:id" {:as req} {:body (retrieve req)})
  (PUT "/MachineConfiguration/:id" {:as req} (update req) {})
  (DELETE "/MachineConfiguration/:id" {:as req} (delete req) {}))