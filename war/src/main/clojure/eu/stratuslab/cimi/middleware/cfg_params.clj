(ns eu.stratuslab.cimi.middleware.cfg-params
  "Middleware that inserts the StratusLab CIMI service configuration
   parameters into the request.  The configuration is available under
   the :sl-cfg-params key in the modified request."
  (:require [cemerick.friend.credentials :as creds]))

(def ^:const default-db-url "http://localhost:8092/default")

(defn wrap-cfg-params
  "Middleware that inserts the StratusLab CIMI service configuration
   parameters into the request.  The configuration is available under
   the :sl-cfg-params key in the modified request.  This is a map 
   containing two parameters:

     :db-url -- the CouchDB/Couchbase URL for the database
     :admin -- authentication map for an administrative user

   NOTE: the default for the db-url parameter (http://localhost:8092/default)
   is only appropriate for testing environments.

   NOTE: The admin credentials should only be used to recover from a 
   situation in which the authentication configuration is corrupted in
   the database. In production running these should be nil."
  ([handler] (wrap-cfg-params nil nil nil handler))
  ([db-url handler] (wrap-cfg-params db-url nil nil handler))
  ([db-url admin-user admin-pswd handler]
    (let [authn-map (if (and admin-user admin-pswd)
                      {admin-user {:identity admin-user
                                   :username admin-user
                                   :password (creds/hash-bcrypt admin-pswd)}}
                      {})
          cfg-map {:db-url (or db-url default-db-url)
                   :admin authn-map}]
      (fn [req]
        (handler (assoc req :sl-cfg-params cfg-map))))))

(defn db-url
  "Retrieve the CouchDB/Couchbase URL from the request."
  [req]
  (get-in req [:sl-cfg-params :db-url]))

(defn admin-authn-map
  "Retrieve the admin authentication map from the request.  In 
   production, this map is normally nil.  This is used to recover
   from a situation when the authentication configuration in the
   database is corrupted."
  [req]
  (get-in req [:sl-cfg-params :admin]))
