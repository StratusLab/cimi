(ns eu.stratuslab.cimi.resources.machine-configurations
  "Utilities for managing the CRUD features for machine configurations."
  (:require [eu.stratuslab.cimi.resources.common :as common]
            [eu.stratuslab.cimi.resources.utils :as utils]
            [clojure.tools.logging :refer [debug info error]]
            [clj-hector.core :refer [put get-rows get-rows-cql-query delete-rows]]
            [clj-hector.serialize :as serial]))

(def ^:const collection-name "machineConfigs")

(def ^:const resource-name "MachineConfiguration")

(def ^:const resource-uri "http://www.dmtf.org/cimi/MachineConfiguration")

(def resource-attrs
     (common/add-common-resource-attrs
      [{:name "cpu"
        :validator :long
        :serializer :long}
       {:name "memory"
        :validator :long
        :serializer :long}
       {:name "disks"
        :validator :utf-8
        :serializer :string}
       {:name "cpuArch"
        :validator :utf-8
        :serializer :string}]))

(def serialize-values
     (common/create-serialize-values-function resource-attrs))

(utils/defn-db update
  "Update the machine configuration; if the row-id is nil, then the
  machine configuration will be created."
  [row-id data]
  (debug "entering machine-configurations/update" row-id data)
  (let [data (utils/set-time-attributes row-id data)
        row-id (or row-id (utils/create-uuid))]
    (put ks resource-name row-id data :n-serializer :keyword :v-serializer :type-inferring)
    row-id))

(utils/defn-db create
  "Create a new machine configuration."
  [data]
  (update ks nil data))

(utils/defn-db retrieve
  "Returns the data associated with the given row-id."
  [row-id]
  (let [rows (get-rows ks resource-name [row-id] :n-serializer :keyword)
        row (first rows)
        row (serialize-values row)]
    (get row row-id)))

(utils/defn-db delete
  "Delete the given machine configuration(s)."
  [row-id]
  (delete-rows ks resource-name [row-id]))

(utils/defn-db list-machine-configurations
  "Provide a list of all machine configurations."
  []
  (let [query (str "SELECT * FROM " resource-name)
        rows (apply get-rows-cql-query ks query {:n-serializer :keyword})
        valid-rows (remove utils/ghost? rows)
        full-map (apply merge valid-rows)]
    (or full-map {})))
