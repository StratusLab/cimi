(ns eu.stratuslab.cimi.middleware.cfg-params
  "Middleware that inserts the StratusLab CIMI service configuration
   parameters into the request.  The configuration is available under
   the :sl-cfg-params key in the modified request."
  (:require [cemerick.friend.credentials :as creds])
  (:import [java.net URI]))

(def default-db-cfg {:nodes [(URI/create "http://localhost:8091/pools")]
                     :bucket "default"
                     :bucket-pswd ""})

(defn wrap-cfg-params
  "Middleware that inserts the StratusLab CIMI service configuration
   parameters into the request.  The configuration is available under
   the :sl-cfg-params key in the modified request.  This is a map 
   containing two parameters:

     :db-cfg -- the Couchbase database configuration
     :admin -- authentication map for an administrative user

   NOTE: the default for the db-cfg parameter is only appropriate for
   testing environments.  It assumes that the default bucket (without
   password) is used and that the database is running on the local
   machine.

   NOTE: The admin credentials should only be used to recover from a 
   situation in which the authentication configuration is corrupted in
   the database. In production running these should be nil."
  ([handler] (wrap-cfg-params nil nil nil handler))
  ([db-cfg handler] (wrap-cfg-params db-cfg nil nil handler))
  ([db-cfg admin-user admin-pswd handler]
    (let [authn-map (if (and admin-user admin-pswd)
                      {admin-user {:identity admin-user
                                   :username admin-user
                                   :password (creds/hash-bcrypt admin-pswd)}}
                      {})
          cfg-map {:db-cfg (or db-cfg default-db-cfg)
                   :admin authn-map}]
      (fn [req]
        (handler (assoc req :sl-cfg-params cfg-map))))))

(defn db-cfg
  "Retrieve the Couchbase configuration from the request.  This is a map
   with keys :nodes, :bucket, and :bucket-pswd."
  [req]
  (get-in req [:sl-cfg-params :db-cfg]))

(defn admin-authn-map
  "Retrieve the admin authentication map from the request.  In 
   production, this map is normally nil.  This is used to recover
   from a situation when the authentication configuration in the
   database is corrupted."
  [req]
  (get-in req [:sl-cfg-params :admin]))
