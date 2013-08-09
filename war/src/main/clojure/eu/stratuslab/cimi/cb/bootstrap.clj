(ns eu.stratuslab.cimi.cb.bootstrap
  "Provides the utility to provide the necessary views and objects
   in the Couchbase database for minimal operation of the CIMI 
   service."
  (:require 
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]))

(defn create-cep
  "Checks to see if the CloudEntryPoint exists in the database;
   if not, it will create one.  The CloudEntryPoint is the core
   resource of the service and must exist."
  [cb-client]
  (try 
    (cep/create cb-client)
    (log/info "created CloudEntryPoint")
    (catch Exception e
      (log/warn "could not create CloudEntryPoint: " (.getMessage e))
      (try
        (cep/retrieve cb-client)
        (log/info "CloudEntryPoint exists")
        (catch Exception e
          (log/error "problem retrieving CloudEntryPoint: " (.getMessage e)))))))

(defn create-views
  "Ensure that the views necessary for searching the database
   are available."
  [cb-client]
  nil)

(defn bootstrap [cb-client]
  (create-cep cb-client)
  (create-views cb-client))

