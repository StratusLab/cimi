(ns eu.stratuslab.cimi.middleware.servlet-request
  "Middleware for pulling out servlet paths from the request and using
these to create the base URL of the application."
  (:require 
    [clojure.tools.logging :as log]))

(defn wrap-servlet-paths
  "Wraps the ring handler to provide the servlet path information
  in :path-info and the context path in :context.  If this is not
  running in a servlet container, this wrapper does nothing."
  [handler]
  (fn [req]
    (let [servlet-request (:servlet-request req)
          path-info (if servlet-request
                      (.getPathInfo servlet-request))
          context (if servlet-request
                    (.getContextPath servlet-request))
          req (assoc req
                :path-info path-info
                :context context)]
      (log/debug (format "path-info=%s; context=%s" path-info context))
      (handler req))))

(defn wrap-base-uri
  "Wraps the ring handler to provide the base URI of the service based
  on the servlet information.  The wrapper wrap-servlet-paths should
  be called before this wrapper if the handler is running within a
  servlet container."
  [handler]
  (fn [req]
    (let [{:keys [scheme server-name server-port context]} req
          context (or context "")
          base-uri (format "%s://%s:%d%s/" (name scheme) server-name server-port context)
          req (assoc req :base-uri base-uri)]
      (log/debug (format "base-uri=%s" base-uri))
      (handler req))))
