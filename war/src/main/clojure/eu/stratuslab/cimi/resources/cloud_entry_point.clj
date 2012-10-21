(ns eu.stratuslab.cimi.resources.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require [eu.stratuslab.cimi.resources.common :as common]
            [eu.stratuslab.cimi.resources.utils :as utils]
            [clojure.tools.logging :refer [debug info warn]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [clj-hector.core :refer [put get-rows get-rows-cql-query delete-rows]]
            [clj-hector.serialize :as serial]))

(def ^:const resource-name "CloudEntryPoint")

(def ^:const resource-uri "http://www.dmtf.org/cimi/CloudEntryPoint")

(def ^:const cf-name "cloud_entry_point")

(def serialize-values (common/create-serialize-values-function common/common-resource-attrs))

(utils/defn-db retrieve
  "Returns the data associated with the CloudEntryPoint.  There is
  exactly one such entry in the database.  The row id of this entry is
  the same as the resource name 'CloudEntryPoint'."
  [baseURI]
  (let [rows (get-rows ks cf-name [resource-name] :n-serializer :keyword)
        row (first rows)
        row (serialize-values row)
        data (get row resource-name)
        data (merge {:resourceURI resource-uri
                     :baseURI baseURI}
                    data)]
    {:body data}))

(utils/defn-db update
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resources cannot be
  changed."
  [data]
  (debug "entering cloud-entry-point/update" data)
  (let [data (utils/set-time-attributes resource-name data)]
    (put ks cf-name resource-name data :n-serializer :keyword :v-serializer :type-inferring)
    resource-name))

(utils/defn-db create
  "Create a new cloud entry point in the database.  This should only
  be called through external utilities used to initialize the
  database."
  [data]
  (update ks nil data))

(defroutes resource-routes
  (GET "/" {:keys [base-url]} (retrieve base-url))
  (PUT "/" {:as data} (update data)))
