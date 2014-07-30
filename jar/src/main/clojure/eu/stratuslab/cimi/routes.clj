(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require
    [eu.stratuslab.cimi.resources.utils.dynamic-load :as dyn]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [compojure.core :refer [defroutes let-routes routes POST GET PUT DELETE ANY]]
    [compojure.route :as route]))

(def collection-routes
  (let-routes [uri "/cimi/:resource-type"]
              (POST uri [resource-type :as {:keys [cb-client body]}]
                    (crud/add resource-type cb-client body))
              (GET uri [resource-type :as {:keys [cb-client body]}]
                   (crud/query resource-type cb-client body))
              (ANY uri []
                   (u/bad-method)))
  )

(def resource-routes
  (let-routes [uri "/cimi/:resource-type/:uuid"]
              (GET uri [resource-type uuid :as {cb-client :cb-client}]
                   (crud/retrieve resource-type cb-client uuid))
              (PUT uri [resource-type uuid :as {cb-client :cb-client body :body}]
                   (crud/edit resource-type cb-client uuid body))
              (DELETE uri [resource-type uuid :as {cb-client :cb-client}]
                      (crud/delete resource-type cb-client uuid))
              (ANY uri []
                   (u/bad-method))))

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
