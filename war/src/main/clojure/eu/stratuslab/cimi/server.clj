(ns eu.stratuslab.cimi.server
  "Entry point for running the VM REST API server."
  (:use [clojure.tools.logging :only [info warn]])
  (:require [noir.server :as noir]
            [eu.stratuslab.cimi.friend-utils :as friend-utils]))

;; The views must be loaded (required) before the handler is generated.
(noir/load-views-ns 'eu.stratuslab.cimi.views)

;; Authentication must also be configured.
;; NOTE: the context path is hardcoded!
;; FIXME: should be "/vm"
(friend-utils/configure-friend friend-utils/credential-fn "")

(def servlet-handler
  "Wraps the standard noir handler to inject the path info and context
  path into the ring request."
  (let [handler (noir/gen-handler {:mode :dev, :ns 'eu.stratuslab.cimi.server})]
    (fn [request]
      (handler
       (assoc request
         :path-info (.getPathInfo (:servlet-request request))
         :context (.getContextPath (:servlet-request request)))))))

(defn init
  [path]

  (info "setting context path to" path)

  (info "starting service configuration thread")

  (info "initializing authentication framework (friend)")
  (friend-utils/configure-friend friend-utils/credential-fn path))
