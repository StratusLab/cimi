(ns eu.stratuslab.cimi.views.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(def resource-name "CloudEntryPoint")

(def resource-uri "http://www.dmtf.org/cimi/CloudEntryPoint")

(defn retrieve
  [baseURI]
  {:resourceURI resource-uri
   :baseURI baseURI})
