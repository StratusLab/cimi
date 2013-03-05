(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]))

(defroutes main-routes
  cep/resource-routes
  (GET "/debug" {:as req} {:body req})
  (route/not-found "Page not found"))
