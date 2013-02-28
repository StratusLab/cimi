(ns eu.stratuslab.cimi.server
  "Entry point for running the StratusLab CIMI interface."
  (:require
    [clojure.tools.logging :as log]
    [compojure.handler :as handler]
    [ring.middleware.format-params :refer [wrap-restful-params]]
    [eu.stratuslab.cimi.middleware.format-response :refer [wrap-restful-response]]
    [eu.stratuslab.cimi.middleware.cfg-params :refer [default-db-url wrap-cfg-params]]
    [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-servlet-paths
                                                           wrap-base-url]]
    [eu.stratuslab.cimi.routes :as routes]
    [eu.stratuslab.cimi.friend-utils :as friend-utils]))

;;
;; Authentication needs to be configured.
;;
(def servlet-handler
  (-> (handler/site routes/main-routes)
      (wrap-base-url)
      (wrap-servlet-paths)
      (wrap-cfg-params)
      (wrap-restful-params)
      (wrap-restful-response)))

(defn init
  "Initialized the servlet with the given parameters.  The keys
   in the configuration parameter map are:

   :path -- context path for the server
   :db-url -- CouchDB/Couchbase URL
   :admin-user -- admin username for server recovery
   :admin-pswd -- admin password for server recovery

   Both the admin username and password must be given to have
   any effect; these should only be used for system recovery."
  [params]

  (let [{:keys [path db-url admin-user admin-pswd]} params
        path (or path "")]
    
    (log/info "server context path is: " path)
    
    (if db-url
      (log/info "using database URL: " db-url)
      (log/warn "using default database URL: " default-db-url))
    
    (if (and admin-user admin-pswd)
      (log/warn "using recovery admin user and password"))))
