(ns eu.stratuslab.cimi.server
  "Implementation of the ring application used to create the
   servlet instance for a web application container."
  (:require
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [compojure.handler :as handler]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [eu.stratuslab.cimi.couchbase-cfg :refer [read-cfg]]
    [eu.stratuslab.authn.workflows.authn-workflows :as aw]
    [eu.stratuslab.cimi.cb.bootstrap :refer [bootstrap]]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
    [eu.stratuslab.cimi.middleware.base-uri :refer [wrap-base-uri]]
    [eu.stratuslab.cimi.middleware.couchbase-store :refer [couchbase-store]]
    [eu.stratuslab.cimi.routes :as routes]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds]
    [metrics.ring.instrument :refer [instrument]]
    [metrics.ring.expose :refer [expose-metrics-as-json]]
    [org.httpkit.server :refer [run-server]])
  (:import
    [java.net URI]))

(def cb-client-defaults {:uris     [(URI/create "http://localhost:8091/pools")]
                         :bucket   "default"
                         :username ""
                         :password ""})

(defn- create-cb-client
  "Creates a Couchbase client instance from the given configuration.
   If the argument is nil, then the default connection parameters
   are used."
  [cb-cfg]
  (log/info "create Couchbase client")
  (if-let [cfg (read-cfg cb-cfg)]
    (try
      (cbc/create-client cfg)
      (catch Exception e
        (log/error "error creating couchbase client" (str e))
        (cbc/create-client cb-client-defaults)))
    (do
      (log/warn "using default couchbase configuration")
      (cbc/create-client cb-client-defaults))))

(defn- create-ring-handler
  "Creates a ring handler that wraps all of the service routes
   in the necessary ring middleware to handle authentication,
   header treatment, and message formatting."
  [{:keys [cb-client context]}]
  (log/info "creating servlet ring handler with context" context)

  (let [workflows (aw/get-workflows cb-client)]
    (if (empty? workflows)
      (log/warn "NO authn workflows configured"))

    (-> (friend/authenticate routes/main-routes
                             {:allow-anon?         true
                              :login-uri           "/login"
                              :default-landing-uri "/webui"
                              :credential-fn       (constantly nil)
                              :workflows           workflows})
        (handler/site {:session {:store (couchbase-store cb-client)}})
        (wrap-base-uri)
        (wrap-cb-client cb-client)
        (instrument)
        (expose-metrics-as-json)
        (wrap-json-body)
        (wrap-json-response))))

(defn- start-container
  "Starts the http-kit container with the given ring application and
   on the given port.  Returns the function to be called to shutdown
   the http-kit container."
  [ring-app port]
  (log/info "starting the http-kit container on port" port)
  (run-server ring-app {:port port}))

(declare stop)

(defn- create-shutdown-hook
  [state]
  (proxy [Thread] [] (run [] (stop state))))

(defn register-shutdown-hook
  "This function registers a shutdown hook to close the database
   client cleanly and to shutdown the http-kit container when the
   JVM exits.  This only needs to be called in a context in which
   the stop function will not be explicitly called."
  [state]
  (let [hook (create-shutdown-hook state)]
    (.. (Runtime/getRuntime)
        (addShutdownHook hook))
    (log/info "registered shutdown hook")))

(defn start
  "Starts the CIMI server and returns a map with the application
   state containing the Couchbase client and the function to stop
   the http-kit container."
  [cb-cfg-file context port]
  (let [cb-client (create-cb-client cb-cfg-file)
        ring-app (create-ring-handler {:cb-client cb-client :context context})
        stop-fn (start-container ring-app port)
        state {:cb-client cb-client :stop-fn stop-fn}]
    (bootstrap cb-client)
    state))

(defn stop
  "Stops the http-kit container and shuts down the Couchbase
   client.  Takes the global state map generated by the start
   function as the argument."
  [{:keys [cb-client stop-fn]}]
  (log/info "shutting down Couchbase client")
  (if cb-client
    (cbc/shutdown cb-client 3000))
  (log/info "shutting down http-kit container")
  (if stop-fn
    (stop-fn)))
