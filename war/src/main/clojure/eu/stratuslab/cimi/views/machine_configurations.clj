(ns eu.stratuslab.cimi.views.machine-configurations
  "Utilities for managing the CRUD features for machine configurations."
  (:import [java.util UUID]
           [java.nio ByteBuffer])
  (:require [eu.stratuslab.cimi.views.utils :as utils]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer [debug info error]]
            [clj-hector.core :refer [put get-rows get-rows-cql-query delete-rows]]
            [clj-hector.serialize :as serial]))

(def ^:const ks-name "stratuslab_cimi")

(def ^:const cf-name "machine_templates")

(def ^:const column-metadata [{:name "id" :validator :utf-8}
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

(defn update
  "Update the machine configuration; if the row-id is nil, then the
  machine configuration will be created."
  ([row-id data]
     (update (utils/ks ks-name) row-id data))

  ([ks row-id data]
     (debug "entering machine-configurations/update" row-id data)
     (let [data (utils/set-time-attributes row-id data)
           row-id (or row-id (utils/create-uuid))]
       (try
         (put ks cf-name row-id data :n-serializer :keyword :v-serializer :type-inferring)
         row-id
         (catch Exception e
           (let [msg (str "error updating/creating machine configuration:" row-id)]
             (.printStackTrace e)
             (error msg "--" (.getMessage e))
             (throw e)))))))

(defn create
  "Create a new machine configuration."
  ([data]
     (create (utils/ks ks-name) data))

  ([ks data]
     (update ks nil data)))

(defn retrieve
  "Returns the data associated with the given row-id."
  ([row-id]
     (retrieve (utils/ks ks-name) row-id))

  ([ks row-id]
     (try
       (let [rows (get-rows ks cf-name [row-id] :n-serializer :keyword)
             row (first rows)
             row (serialize-values row)]
         (pprint rows)
         (pprint row)
         (get row row-id))
       (catch Exception e
         (let [msg (str "retrieve machine configuration error :" row-id)]
           (error msg "--" (.getMessage e))
           (throw e))))))

(defn delete
  "Delete the given machine configuration."
  ([row-id]
     (delete (utils/ks ks-name) row-id))

  ([ks row-id]
     (try
       (delete-rows ks cf-name [row-id])
       (catch Exception e
         (let [msg (str "delete machine configuration error: " row-id)]
           (error msg "--" (.getMessage e))
           (throw e))))))

(defn list-machine-configurations
  "Provide a list of all machine configurations."
  ([]
     (list-machine-configurations (utils/ks ks-name)))

  ([ks]
     (try
       (let [query (str "SELECT * FROM " cf-name)
             rows (apply get-rows-cql-query ks query {:n-serializer :keyword})
             valid-rows (remove utils/ghost? rows)
             full-map (apply merge valid-rows)]
         (if (nil? full-map)
           {}
           full-map))
       (catch Exception e
         (let [msg "list machine configurations error"]
           (error msg "--" (.getMessage e))
           (throw e))))))

