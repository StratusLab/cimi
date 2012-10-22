(ns eu.stratuslab.cimi.server
  "Entry point for running the StratusLab CIMI interface."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.handler :as handler]
            [ring.middleware.format-params :refer [wrap-restful-params]]
            [eu.stratuslab.cimi.middleware.format-response :refer [wrap-restful-response]]
            [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-servlet-paths
                                                                   wrap-base-url]]
            [eu.stratuslab.cimi.routes :as routes]
            [eu.stratuslab.cimi.friend-utils :as friend-utils]
            [eu.stratuslab.cimi.serial.external :as serial]))

;; Authentication must also be configured.
;; NOTE: the context path is hardcoded!
;; FIXME: should be "/vm"
;;(friend-utils/configure-friend friend-utils/credential-fn "")

(def servlet-handler
  (-> (handler/site routes/main-routes)
      (wrap-base-url)
      (wrap-servlet-paths)
      (wrap-restful-params)
      (wrap-restful-response)))

(defn init
  [path]

  (info "setting context path to" path)

  (info "starting service configuration thread")

  (info "initializing authentication framework (friend)")
  #_(friend-utils/configure-friend friend-utils/credential-fn path))
