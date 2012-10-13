(ns eu.stratuslab.cimi.server
  "Entry point for running the StratusLab CIMI interface."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.handler :as handler]
            [eu.stratuslab.cimi.routes :as routes]
            [eu.stratuslab.cimi.friend-utils :as friend-utils]))

;; Authentication must also be configured.
;; NOTE: the context path is hardcoded!
;; FIXME: should be "/vm"
;;(friend-utils/configure-friend friend-utils/credential-fn "")

(defn wrap-servlet-paths
  "Wraps the ring handler to provide the servlet path information
  in :path-info and the context path in :context.  If this is not
  running in a servlet container, this wrapper does nothing."
  [handler]
  (fn [req]
    (let [servlet-request (:servlet-request req)
          req (if servlet-request
                (assoc req
                  :path-info (.getPathInfo servlet-request)
                  :context (.getContextPath servlet-request))
                req)]
      (handler req))))

(defn wrap-base-url
  "Wraps the ring handler to provide the base URL of the service based
  on the servlet information.  The wrapper wrap-servlet-paths should
  be called before this wrapper if the handler is running within a
  servlet container."
  [handler]
  (fn [req]
    (let [{:keys [scheme server-name server-port context]} req
          context (or context "")
          base-url (format "%s://%s:%d%s/" (name scheme) server-name server-port context)
          req (assoc req :base-url base-url)]
      (handler req))))

(def servlet-handler
  (-> (handler/site routes/main-routes)
      (wrap-base-url)
      (wrap-servlet-paths)))

(defn init
  [path]

  (info "setting context path to" path)

  (info "starting service configuration thread")

  (info "initializing authentication framework (friend)")
  #_(friend-utils/configure-friend friend-utils/credential-fn path))
