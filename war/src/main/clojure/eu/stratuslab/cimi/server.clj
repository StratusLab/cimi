(ns eu.stratuslab.cimi.server
  "Entry point for running the StratusLab CIMI interface."
  (:require
    [clojure.tools.logging :as log]
    [clojure.edn :as edn]
    [compojure.handler :as handler]
    [ring.middleware.format-params :refer [wrap-restful-params]]
    [eu.stratuslab.cimi.cb.utils :as cb-utils]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.middleware.format-response :refer [wrap-restful-response]]
    [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
    [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-servlet-paths
                                                           wrap-base-uri]]
    [eu.stratuslab.cimi.routes :as routes]))

(defn- create-cb-client
  "Creates a Couchbase client instance from the given configuration.
   If the argument is nil, then the default connection parameters 
   ('default' bucket on local Couchbase) are used."
  [couchbase]
  (try
    (cb-utils/create-client (edn/read-string couchbase))
    (catch Exception e
      (log/error "error creating couchbase client from configuration: " e)
      (cb-utils/create-client {}))))

(defn create-ring-handler
  "Creates a ring handler that wraps all of the service routes
   in the necessary ring middleware to handle authentication,
   header treatment, and message formatting."
  [{:keys [cb-client]}]
  (log/info "creating servlet ring handler")
  ;; TODO: Authentication needs to be configured!
  (-> (handler/site routes/main-routes)
      (wrap-base-uri)
      (wrap-servlet-paths)
      (wrap-cb-client cb-client)
      (wrap-restful-params)
      (wrap-restful-response)))

(defn init
  "Bootstraps the database if necessary and warns if the service
   is configured to use a hardcoded administrator username and 
   password.

   The db-cfg parameter must be a clojure-formatted (i.e. EDN)
   map of the Couchbase configuration parameters.  Defaults will
   be used if the information is absent or invalid.

   Both the admin username and password must be given to have
   any effect; these should only be used for system recovery.

   The method returns a map of configuration parameters that 
   must be passed to the handler creation method.

   NOTE: The caller is responsible for shutting down the Couchbase 
   client that is created by this method!"
  [{:keys [couchbase]}]
  
  (log/info "initializing servlet implementation")

  (let [cb-client (create-cb-client couchbase)]
    (cep/bootstrap cb-client)    
    {:cb-client cb-client}))

(defn destroy
  "Cleans up resources before shutting down the service. Notably
   terminates the Couchbase client cleanly."
  [{:keys [cb-client]}]
  (log/info "releasing resources and destroying servlet implementation")
  (if cb-client
    (cb-utils/shutdown cb-client)))
