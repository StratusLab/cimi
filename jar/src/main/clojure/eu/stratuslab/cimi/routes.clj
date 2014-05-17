(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require
    [eu.stratuslab.cimi.resources.utils.dynamic-load :as dyn]
    [compojure.core :refer [routes]]
    [compojure.route :as route]))

(def final-routes
  [(route/resources "/cimi/")
   (route/not-found "unknown resource")])

(defn get-main-routes
  "Returns all of the routes defined for the CIMI server.  This uses
   dynamic loading to discover all of the defined resources on the
   classpath."
  []
  (apply routes (doall (concat (dyn/resource-routes) final-routes))))
