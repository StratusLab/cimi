(ns eu.stratuslab.cimi.server
  "Entry point for running the StratusLab CIMI interface."
  (:require
    [clojure.tools.logging :as log]
    [compojure.handler :as handler]
    [ring.middleware.format-params :refer [wrap-restful-params]]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.middleware.format-response :refer [wrap-restful-response]]
    [eu.stratuslab.cimi.middleware.cfg-params :refer [default-db-cfg wrap-cfg-params]]
    [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-servlet-paths
                                                           wrap-base-uri]]
    [eu.stratuslab.cimi.routes :as routes]))

;;
;; Authentication needs to be configured.
;;
(def servlet-handler
  (-> (handler/site routes/main-routes)
      (wrap-base-uri)
      (wrap-servlet-paths)
      (wrap-cfg-params)
      (wrap-restful-params)
      (wrap-restful-response)))

(defn init
  "Initialized the servlet with the given parameters.  The keys
   in the configuration parameter map are:

   :path -- context path for the server
   :db-cfg -- Couchbase configuration parameters
   :admin-user -- admin username for server recovery
   :admin-pswd -- admin password for server recovery

   Both the admin username and password must be given to have
   any effect; these should only be used for system recovery."
  [params]

  (let [{:keys [path db-cfg admin-user admin-pswd]} params
        path (or path "")]
    
    (log/info "server context path is: " path)
    
    (if db-cfg
      (do 
        (log/info "using database configuration: " db-cfg)
        (cep/bootstrap db-cfg))
      (do 
        (log/warn "using default database configuration: " default-db-cfg)
        (cep/bootstrap default-db-cfg)))
    
    (if (and admin-user admin-pswd)
      (log/warn "using recovery admin user and password"))))
