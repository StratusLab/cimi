(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require
    [eu.stratuslab.cimi.resources.utils.dynamic-load :as dyn]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [compojure.core :refer [defroutes let-routes routes POST GET PUT DELETE ANY]]
    [compojure.route :as route]))

(def collection-routes
  (let-routes [uri "/cimi/:resource-name"]
              (POST uri request
                    (crud/add request))
              (GET uri request
                   (crud/query request))
              (ANY uri request
                   (u/bad-method request))))

(def resource-routes
  (let-routes [uri "/cimi/:resource-name/:uuid"]
              (GET uri request
                   (crud/retrieve request))
              (PUT uri request
                   (crud/edit request))
              (DELETE uri request
                      (crud/delete request))
              (ANY uri request
                   (u/bad-method request))))

(def final-routes
  [collection-routes
   resource-routes
   (route/not-found "unknown resource")])

(defn get-main-routes
  "Returns all of the routes defined for the CIMI server.  This uses
   dynamic loading to discover all of the defined resources on the
   classpath."
  []
  (apply routes (doall (concat [(route/resources "/cimi/static")]
                               (dyn/resource-routes)
                               final-routes))))
