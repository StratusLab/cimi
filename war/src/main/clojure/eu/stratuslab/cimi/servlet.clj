(ns eu.stratuslab.cimi.servlet
  "The servlet created by this class dynamically loads the actual
   implementation to avoid having to AOT compile the complete
   application.  The namespace and methods are hardcoded here, but
   could be changed to read values from the ServletContext if more
   flexibility is needed."
  (:require [ring.util.servlet :refer [make-service-method]])
  (:gen-class
   :extends javax.servlet.http.HttpServlet
   :init initState
   :state state))

(defn- servlet-params [this]
  {:cb-cfg (.getInitParameter this "cb-cfg")})

(defn -update-state [this params]
  (swap! (.state this) merge params))

(defn -initState [] [[] (atom {})])

(defn -init-void [this]
  (let [n (symbol "eu.stratuslab.cimi.server")
        handler-fn (symbol "create-ring-handler")
        init-fn (symbol "init")
        destroy-fn (symbol "destroy")]
    (require n)  ;; this dynamically loads the server impl.
    (-update-state this
      {:handler-fn (ns-resolve (the-ns n) handler-fn)
       :init-fn (ns-resolve (the-ns n) init-fn)
       :destroy-fn (ns-resolve (the-ns n) destroy-fn)}))

  (let [init-fn (-> this .state deref :init-fn)
        servlet-params (servlet-params this)
        cfg-params (init-fn servlet-params)]
    (-update-state this 
      {:cfg-params cfg-params}))
  
  (let [handler-fn (-> this .state deref :handler-fn)
        cfg-params (-> this .state deref :cfg-params)]
    (-update-state this
      {:service-fn (make-service-method (handler-fn cfg-params))})))

(defn -destroy-void
  [this]
  (let [destroy-fn (-> this .state deref :destroy-fn)
        cfg-params (-> this .state deref :cfg-params)]
    (destroy-fn cfg-params)))

(defn -service
  [this req resp]
  ((-> this .state deref :service-fn) this req resp))
