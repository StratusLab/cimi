(ns eu.stratuslab.cimi.middleware.cb-client
  "Middleware that inserts the configured Couchbase client into the
   request.")

(defn wrap-cb-client
  "Middleware that inserts the configured Couchbase client into the 
   request.  The client can be retrieved from the request with the
   :cb-client key."
  ([handler cb-client]
    (fn [req]
      (handler (assoc req :cb-client cb-client)))))
