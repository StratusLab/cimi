(ns eu.stratuslab.cimi.views.ring-request-map
  "Debugging resource showing the complete ring request map."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defn retrieve
  [req]
  req)
