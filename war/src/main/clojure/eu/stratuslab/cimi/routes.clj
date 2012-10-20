(ns eu.stratuslab.cimi.routes
  "Primary routing table for CIMI application."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [eu.stratuslab.cimi.resources.cloud-entry-point :as cloud-entry-point]))

(defroutes main-routes
  (GET "/" {:keys [base-url]} (cloud-entry-point/retrieve base-url))
  (GET "/debug" {:as req} {:body req})
  (route/resources "/")
  (route/not-found "Page not found"))
