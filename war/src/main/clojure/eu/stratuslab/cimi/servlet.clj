(ns eu.stratuslab.cimi.servlet
  "The servlet created by this class dynamically loads the actual
   implementation to avoid having to AOT compile the complete
   application.  The namespace and methods are hardcoded here, but
   could be changed to read values from the ServletContext if more
   flexibility is needed."
  (:require [ring.util.servlet :as ring]
            [clojure.tools.logging :as log])
  (:gen-class
   :extends javax.servlet.http.HttpServlet
   :init initState
   :state state))

(defn -initState [] [[] (atom {})])

(defn -init-void [this]
  (log/info "loading servlet implementation and initializing state")
  (let [n (symbol "eu.stratuslab.cimi.server")
        servlet-handler (symbol "servlet-handler")
        init-fn (symbol "init")]
    (require n)
    (swap!
     (.state this)
     merge
     {:service-fn (ring/make-service-method (ns-resolve (the-ns n) servlet-handler))
      :init-fn (ns-resolve (the-ns n) init-fn)})
    (log/info "initializing the servlet"))
  ((-> this .state deref :init-fn) "/cimi"))

(defn -destroy-void
  [this]
  (log/info "destroying servlet instance"))

(defn -service
  [this req resp]
  ((-> this .state deref :service-fn) this req resp))
