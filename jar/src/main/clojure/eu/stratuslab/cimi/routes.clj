(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [clojure.java.classpath :as cp]
    [clojure.tools.namespace.find :as nsf]))

(defn cimi-resource?
  "If the given symbol represents a resource namespace, the symbol
   is returned; nil otherwise.  Resource namespaces have the prefix
   'eu.stratuslab.cimi.resources.'. "
  [sym]
  (->> (name sym)
       (re-matches #"^eu\.stratuslab\.cimi\.resources\.[\w-]+$")
       (first)))

(defn resources
  "Returns the namespaces of all CIMI resources available on the
   classpath."
  []
  (->> (cp/classpath)
       (nsf/find-namespaces)
       (filter cimi-resource?)))

(defn load-resource
  "Dynamically loads the given namespace, returning the namespace.
   Will return nil if the namespace could not be loaded."
  [resource-ns]
  (try
    (require resource-ns)
    (log/info "loaded resource namespace:" (name resource-ns))
    resource-ns
    (catch Exception e
      (log/warn "could not load resource namespace:" (name resource-ns)))))

(defn get-routes-var
  "Retrieves the 'routes' var in the given namespace, returning
   nil if the var could not be found."
  [resource-ns]
  (if-let [routes (-> resource-ns
                      (name)
                      (str "/routes")
                      (symbol)
                      (find-var))]
    (do
      (log/info "retrieved routes for" (name resource-ns))
      routes)
    (do
      (log/warn "did NOT retrieve routes for" (name resource-ns)))))

(defn resource-routes
  "Returns a lazy sequence of all of the routes for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-resource)
       (map get-routes-var)
       (remove nil?)
       (map deref)))

(def final-routes
  [(route/resources "/cimi/")
   (route/not-found "unknown resource")])

(defn get-main-routes
  "Returns all of the routes defined for the CIMI server.  This uses
   dynamic loading to discover all of the defined resources on the
   classpath."
  []
  (apply routes (doall (concat (resource-routes) final-routes))))
