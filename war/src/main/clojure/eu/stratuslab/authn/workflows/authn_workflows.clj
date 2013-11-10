(ns eu.stratuslab.authn.workflows.authn-workflows
  (:require
    [clojure.tools.logging :as log]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    [couchbase-clj.client :as cbc]
    [cemerick.friend :as friend]
    [cemerick.friend.util :as futil]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds]
    [ring.util.request :as req]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.authn.workflows.client-certificate :as cwf]
    [eu.stratuslab.authn.ldap :as ldap]
    [eu.stratuslab.cimi.resources.schema :as schema]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]))

(def-map-schema LocalDBSchema
                [[:password-enabled] Boolean
                 [:cert-enabled] Boolean])

(def-map-schema VomsSchema
                [[:enabled] Boolean])

(def-map-schema LdapSchema
                ldap/LdapConfigurationSchema
                [[:password-enabled] Boolean
                 [:cert-enabled] Boolean])

(def-map-schema AuthnConfigurationSchema
                schema/CommonAttrs
                [[:localdb] LocalDBSchema
                 (optional-path [:ldap]) NonEmptyString
                 (optional-path [:voms]) NonEmptyString])

(defn config-errors?
  [m]
  (let [errors (validation-errors AuthnConfigurationSchema m)]
    (if (pos? (count errors))
      (str/join ", " errors))))

(def default-cfg
  {:localdb {:password-enabled true
             :cert-enabled     false}})

(defn authn-cfg
  "Finds the configuration file for the authentication (docid in
   Couchbase is 'ServiceConfiguration/authn').  The returned value
   contains the validated configuration."
  [cb-client]
  (if-let [cfg (u/service-configuration cb-client "authn")]
    (if-let [error-msg (config-errors? cfg)]
      (do
        (log/error "ServiceConfiguration/authn error; using defaults:" error-msg)
        default-cfg)
      (do
        (log/info "validated ServiceConfiguration/authn")
        cfg))
    (do
      (log/info "ServiceConfiguration/authn NOT found; using defaults")
      default-cfg)))

(defn form-workflow
  "Creates a friend login workflow that redirects to a login page and
   then processes a POST to the same page with credentials.  Unlike the
   standard interactive form workflow, this workflow also processes
   certificate-based credentials.

   The credential map provided to the defined credential function may
   contain :username, :password, and :ssl-client-cert-chain keys."
  [& {:keys [credential-fn login-failure-handler redirect-on-auth?] :as form-config
      :or   {redirect-on-auth? true}}]
  (fn [{:keys [request-method params form-params servlet-request] :as request}]
    (let [ssl-client-cert-chain (cwf/extract-client-cert-chain servlet-request)
          req-auth-config (::friend/auth-config request)]
      (when (and (= :post request-method)
                 (= (futil/gets :login-uri form-config req-auth-config) (req/path-info request)))
        (let [creds {:username              (get form-params "username" "")
                     :password              (:password params)
                     :ssl-client-cert-chain ssl-client-cert-chain}
              credential-fn (futil/gets :credential-fn form-config req-auth-config)]
          (when-let [user-record (credential-fn
                                   (with-meta creds {::friend/workflow :form-workflow}))]
            (log/debug "user-record is" user-record)
            (workflows/make-auth user-record
                                 {::friend/workflow          :form-workflow
                                  ::friend/redirect-on-auth? redirect-on-auth?})))))))

(defn cb-lookup-user
  "Looks up a user record (map) given a particular key.  This key
   can be the user's identity or any given alternative names, for
   example, an X500 DN."
  [cb-client userkey]
  (when userkey
    (if-let [user-map (u/user-record cb-client userkey)]
      (do
        (log/debug "user record for" userkey "found; active?" (:active user-map))
        (if (:active user-map)
          user-map))
      (log/debug "user record for" userkey "NOT found"))))

(defn cb-user-by-vo-fn
  "Returns a function that constructs the user record from the
   information in a VOMS certificate chain."
  [cb-client]
  (fn [{:keys [ssl-client-cert-chain]}]
    (let [dn (cwf/extract-subject-dn ssl-client-cert-chain)
          voms-info (cwf/voms-vo-info ssl-client-cert-chain)
          vo-names (->> voms-info
                        (keys)
                        (filter #(cb-lookup-user cb-client (str "vo:" %))))
          all-roles (->> vo-names
                         (map #(get voms-info %))
                         (reduce concat))]
      (log/debug "looking up VOs for DN" dn)
      (log/debug "vo-names" vo-names)
      (log/debug "all-roles" all-roles)
      (if (pos? (count vo-names))
        {:identity dn
         :vo-names vo-names
         :roles    all-roles}))))

(defn cb-user-by-id-fn
  "Returns a function that returns a user record with a
   document id of 'User/identity' in the database.  If
   the document doesn't exist or the :active flag is not
   set, then the function returns nil."
  [cb-client]
  (fn [identity]
    (log/debug "looking up identity" identity)
    (cb-lookup-user cb-client identity)))

(defn cb-user-by-dn-fn
  "Returns a function that retrieves a user record in the
   Couchbase database corresponding to the DN of the subject
   in the given X509 certificate chain.  This method should be
   called with a map with the key :ssl-client-cert containing
   the certificate chain."
  [cb-client]
  (fn [{:keys [ssl-client-cert-chain]}]
    (let [dn (cwf/extract-subject-dn ssl-client-cert-chain)]
      (log/debug "looking up DN" dn)
      (cb-lookup-user cb-client dn))))

(defn voms-workflow
  "Returns a workflow that tests the VOMS proxy provided
   with the request.  The proxy will have been validated by
   the SSL interactions before the workflow receives the certificate.
   The result contains the DN as the identity, the FQANs as the
   roles, and the list of accepts VOs."
  [cb-client cfg]
  (when (:enabled cfg)
    (log/info "initializing voms proxy authn workflow")
    (->> cb-client
         (cb-user-by-dn-fn)
         (form-workflow :credential-fn))))

(defn cert-ldap-workflow
  "Returns a workflow that tests the client certificate provided
   with the request.  The certificate will have been validated by
   the SSL interactions before the workflow receives the certificate."
  [ldap-params]
  (when (:cert-enabled ldap-params)
    (if true
      (do
        (log/error "ldap certificate authn workflow is NOT implemented")
        nil)
      (when-let [pool (ldap/connection-pool (:connection ldap-params))]
        (log/info "initializing ldap certificate authn workflow")
        (->> pool
             (assoc ldap-params :ldap-connection-pool)
             (partial ldap/ldap-cert-credential-fn)
             (form-workflow :credential-fn))))))

(defn cert-workflow
  "Returns a workflow that tests the client certificate provided
   with the request.  The certificate will have been validated by
   the SSL interactions before the workflow receives the certificate."
  [cb-client cfg]
  (when (:cert-enabled cfg)
    (log/info "initializing couchbase certificate authn workflow")
    (->> cb-client
         (cb-user-by-vo-fn)
         (form-workflow :credential-fn))))

(defn password-ldap-workflow
  "Returns the an interactive form workflow for friend that
   is configured to find user information in Couchbase."
  [ldap-params]
  (when (:password-enabled ldap-params)
    (when-let [pool (ldap/connection-pool (:connection ldap-params))]
      (log/info "initializing ldap password authn workflow")
      (->> pool
           (assoc ldap-params :ldap-connection-pool)
           (partial ldap/ldap-credential-fn)
           (form-workflow :credential-fn)))))

(defn password-workflow
  "Returns the an interactive form workflow for friend that
   is configured to find user information in Couchbase."
  [cb-client cfg]
  (when (:password-enabled cfg)
    (log/info "initializing couchbase password authn workflow")
    (->> cb-client
         (cb-user-by-id-fn)
         (partial creds/bcrypt-credential-fn)
         (form-workflow :credential-fn))))

(defn get-workflows
  "Returns a list of the active workflows for authenticating
   users for the cloud instance."
  [cb-client]
  (let [cfg (authn-cfg cb-client)]
    (let [workflows [(password-workflow cb-client (:localdb cfg))
                     (password-ldap-workflow (:ldap cfg))
                     (cert-workflow cb-client (:localdb cfg))
                     (cert-ldap-workflow (:ldap cfg))
                     (voms-workflow cb-client (:voms cfg))]]
      (remove nil? workflows))))

