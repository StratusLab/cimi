(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
            [eu.stratuslab.cimi.resources.volume :as volume]
            [eu.stratuslab.cimi.resources.job :as job]
            [eu.stratuslab.cimi.resources.machine-configuration :as mc]))

(defroutes main-routes
  cep/resource-routes
  mc/resource-routes
  job/resource-routes
  volume/resource-routes
  (GET "/debug" {:as req} {:body req})
  (route/not-found "Page not found"))
