(ns eu.stratuslab.cimi.resources.machine-configurations
  "Utilities for managing the CRUD features for machine configurations."
  (:import [java.util UUID]
           [java.nio ByteBuffer])
  (:require [eu.stratuslab.cimi.resources.utils :as utils]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer [debug info error]]
            [clj-hector.core :refer [put get-rows get-rows-cql-query delete-rows]]
            [clj-hector.serialize :as serial]))

(def ^:const cf-name "machine_templates")

(def ^:const column-metadata
     [{:name "id" :validator :utf-8}
      {:name "name" :validator :utf-8}
      {:name "description" :validator :utf-8}
      {:name "created" :validator :long}
      {:name "updated" :validator :long}
      {:name "cpu" :validator :long}
      {:name "memory" :validator :long}
      {:name "cpuArch" :validator :utf-8}])

(def serialize-values
     (utils/create-serialize-values-function
      {:created :date
       :updated :date
       :cpu :long
       :memory :long
       :default :string}))

(utils/defn-db update
  "Update the machine configuration; if the row-id is nil, then the
  machine configuration will be created."
  [row-id data]
  (debug "entering machine-configurations/update" row-id data)
  (let [data (utils/set-time-attributes row-id data)
        row-id (or row-id (utils/create-uuid))]
    (put ks cf-name row-id data :n-serializer :keyword :v-serializer :type-inferring)
    row-id))

(utils/defn-db create
  "Create a new machine configuration."
  [data]
  (update ks nil data))

(utils/defn-db retrieve
  "Returns the data associated with the given row-id."
  [row-id]
  (let [rows (get-rows ks cf-name [row-id] :n-serializer :keyword)
        row (first rows)
        row (serialize-values row)]
    (pprint rows)
    (pprint row)
    (get row row-id)))

(utils/defn-db delete
  "Delete the given machine configuration."
  [row-id]
  (delete-rows ks cf-name [row-id]))

(utils/defn-db list-machine-configurations
  "Provide a list of all machine configurations."
  []
  (let [query (str "SELECT * FROM " cf-name)
        rows (apply get-rows-cql-query ks query {:n-serializer :keyword})
        valid-rows (remove utils/ghost? rows)
        full-map (apply merge valid-rows)]
    (or full-map {})))
