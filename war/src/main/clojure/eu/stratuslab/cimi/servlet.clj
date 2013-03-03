(ns eu.stratuslab.cimi.servlet
  "The servlet created by this class dynamically loads the actual
   implementation to avoid having to AOT compile the complete
   application.  The namespace and methods are hardcoded here, but
   could be changed to read values from the ServletContext if more
   flexibility is needed."
  (:require [ring.util.servlet :as ring]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn])
  (:gen-class
   :extends javax.servlet.http.HttpServlet
   :init initState
   :state state))

(defn- servlet-params [this]
  (let [db-cfg-edn (.getInitParameter this "db-cfg")
        db-cfg (if db-cfg-edn
                 (edn/read-string db-cfg-edn))]
    {:path "/cimi"
     :db-cfg db-cfg
     :admin-user (.getInitParameter this "admin-user")
     :admin-pswd (.getInitParameter this "admin-pswd")}))

(defn -initState [] [[] (atom {})])

(defn -init-void [this]
  (let [n (symbol "eu.stratuslab.cimi.server")
        servlet-handler (symbol "servlet-handler")
        init-fn (symbol "init")]
    (log/info "loading servlet implementation and initializing state")
    (require n)
    (swap!
      (.state this)
      merge
      {:service-fn (ring/make-service-method (ns-resolve (the-ns n) servlet-handler))
       :init-fn (ns-resolve (the-ns n) init-fn)}))

  (log/info "initializing the servlet")
  ((-> this .state deref :init-fn) (servlet-params this)))

(defn -destroy-void
  [this]
  (log/info "destroying servlet instance"))

(defn -service
  [this req resp]
  ((-> this .state deref :service-fn) this req resp))
