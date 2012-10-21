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

(def serialize-values (common/create-serialize-values-function common/common-resource-attrs))

(defn- insert-data
  [ks data]
  (put ks resource-name resource-name data
       :n-serializer :keyword :v-serializer :type-inferring))

(defn- get-data
  [ks baseURI]
  (let [rows (get-rows ks resource-name [resource-name]
                       :n-serializer :keyword)]
    (-> (first rows)
        (serialize-values)
        (get resource-name)
        (merge {:resourceURI resource-uri
                :baseURI baseURI}))))

(utils/defn-db initialize
  "Initialize the cloud entry point.  Calling this when the record
  already exists will cause the name, description, create time, and
  update time to be reset.  Returns the inserted data."
  []
  (let [data (utils/set-time-attributes
              nil
              {:id resource-name
               :name "StratusLab Cloud"
               :description "StratusLab Cloud"})]
    (insert-data ks data)
    data))

(utils/defn-db retrieve
  "Returns the data associated with the CloudEntryPoint.  There is
  exactly one such entry in the database.  The row id of this entry is
  the same as the resource name 'CloudEntryPoint'.  The baseURI must
  be passed as this is taken from the ring request."
  [baseURI]
  (get-data ks baseURI))

(utils/defn-db update
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [data]
  (let [data (utils/set-time-attributes true data)]
    (insert-data ks data)))

(defroutes resource-routes
  (GET "/" {:keys [base-url]} {:body (retrieve base-url)})
  (PUT "/" {:as data} (update data) {}))
