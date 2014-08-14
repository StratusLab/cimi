(ns eu.stratuslab.cimi.db.cb.bootstrap
  "Provides the utility to provide the necessary views and objects
   in the Couchbase database for minimal operation of the CIMI
   service."

  (:require
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.resources.user :as user]
    [eu.stratuslab.cimi.db.cb.views :as views]
    [eu.stratuslab.cimi.db.cb.utils :as cbutils]
    [couchbase-clj.client :as cbc]
    [cemerick.friend.credentials :as creds]
    [eu.stratuslab.cimi.resources.common.crud :as crud]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [cemerick.friend :as friend]))

(defn create-views
  "Ensure that the views necessary for searching the database
   are available."
  [cb-client]
  (if (views/add-design-doc cb-client)
    (log/info "design document added to database")
    (log/info "design document NOT added to database")))

(defn bootstrap
  "Bootstraps the Couchbase database by creating the CloudEntryPoint
   and inserting the design document with views, as necessary."
  [cb-client]
  (cbutils/wait-until-ready cb-client)
  (create-views cb-client)
  (views/views-available? cb-client))
