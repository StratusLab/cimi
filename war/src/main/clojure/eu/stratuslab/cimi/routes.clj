(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
            [eu.stratuslab.cimi.resources.job :as job]
            [eu.stratuslab.cimi.resources.machine-configuration :as mc]
            [eu.stratuslab.cimi.resources.service-message :as sm]
            [eu.stratuslab.cimi.resources.volume :as v]
            [eu.stratuslab.cimi.resources.volume-template :as vt]
            [eu.stratuslab.cimi.resources.volume-configuration :as vc]
            [eu.stratuslab.cimi.resources.volume-image :as vi]))

(defroutes main-routes
           cep/routes
           mc/routes
           job/routes
           sm/routes
           v/routes
           vt/routes
           vc/routes
           vi/routes
           (GET "/debug" {:as req} {:body req})
           (route/not-found "Page not found"))
