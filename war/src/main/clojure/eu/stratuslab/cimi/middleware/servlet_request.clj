(ns eu.stratuslab.cimi.middleware.servlet-request
  "Middleware for pulling out servlet paths from the request and using
these to create the base URL of the application.")

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
