(ns eu.stratuslab.cimi.authn-workflows
  (:require
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds]
    [eu.stratuslab.cimi.resources.utils :as u]))

(defn cb-users-fn
  "Returns a function that returns a user record with a
   document id of 'User/identity' in the database.  If
   the document doesn't exist or the :active flag is not
   set, then the function returns nil."
  [cb-client]
  (fn [identity]
    (log/debug "trying to authenticate user:" identity)
    (if-let [user-map (u/user-record cb-client identity)]
      (do
        (log/debug "found user information for: " identity "->" user-map)
        (if (:active user-map)
          user-map)))))

(defn form-workflow
  "Returns the an interactive form workflow for friend that
   is configured to find user information in Couchbase."
  [cb-client]
  (->> cb-client
       (cb-users-fn)
       (partial creds/bcrypt-credential-fn)
       (workflows/interactive-form :credential-fn)))

(defn get-workflows
  "Returns a list of the active workflows for authenticating
   users for the cloud instance."
  [cb-client]
  [(form-workflow cb-client)])

