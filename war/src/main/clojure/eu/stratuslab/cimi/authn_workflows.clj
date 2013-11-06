(ns eu.stratuslab.cimi.authn-workflows
  (:require
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [pprint]]
    [couchbase-clj.client :as cbc]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.authn.workflows.client-certificate :as cwf]))

(defn cb-lookup-user
  "Looks up a user record (map) given a particular key.  This key
   can be the user's identity or any given alternative names, for
   example, an X500 DN."
  [cb-client userkey]
  (if userkey
    (if-let [user-map (u/user-record cb-client userkey)]
      (do
        (log/info "user record for" userkey "found; active?" (:active user-map))
        (if (:active user-map)
          user-map))
      (log/info "user record for" userkey "NOT found"))))

(defn user-by-id-fn
  "Returns a function that returns a user record with a
   document id of 'User/identity' in the database.  If
   the document doesn't exist or the :active flag is not
   set, then the function returns nil."
  [cb-client]
  (fn [identity]
    (cb-lookup-user cb-client identity)))

(defn user-by-dn-fn
  "Returns a function that retrieves a user record in the
   Couchbase database corresponding to the DN of the subject
   in the given X509 certificate chain.  This method should be
   called with a map with the key :ssl-client-cert containing
   the certificate chain."
  [cb-client]
  (fn [{:keys [ssl-client-cert servlet-request] :as request}]
    (let [dn (cwf/extract-subject-dn ssl-client-cert servlet-request)]
      (cb-lookup-user cb-client dn))))

(defn cert-workflow
  "Returns a workflow that tests the client certificate provided
   with the request.  The certificate will have been validated by
   the SSL interactions before the workflow receives the certificate."
  [cb-client]
  (->> cb-client
       (user-by-dn-fn)
       (cwf/client-certificate :credential-fn)))

(defn form-workflow
  "Returns the an interactive form workflow for friend that
   is configured to find user information in Couchbase."
  [cb-client]
  (->> cb-client
       (user-by-id-fn)
       (partial creds/bcrypt-credential-fn)
       (workflows/interactive-form :credential-fn)))

(defn get-workflows
  "Returns a list of the active workflows for authenticating
   users for the cloud instance."
  [cb-client]
  [(cert-workflow cb-client)
   (form-workflow cb-client)])

