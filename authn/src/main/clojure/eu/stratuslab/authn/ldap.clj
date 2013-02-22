(ns eu.stratuslab.authn.ldap
  "Functions to allow Friend authentication against an LDAP database."
  (:require [clojure.string :as str]
    [clj-ldap.client :as ldap]))

(def ldap-params {:host {:address "localhost" :port 10389}
                  :bind-dn "cn=admin,o=cloud"
                  :password "secret"
                  :ssl? false
                  :connect-timeout 2000
                  :timeout 2000})

(declare ldap-pool)
(comment (def ldap-pool (ldap/connect ldap-params)))

(def ^:const ldap-defaults
  {:user-base-dn "ou=users,o=cloud"
   :user-object-class "inetOrgPerson"
   :user-id-attr "uid"
   
   :role-base-dn "ou=groups,o=cloud"
   :role-object-class "groupOfUniqueNames"
   :role-member-attr "uniqueMember"
   :role-name-attr :cn ;; must be a keyword
   })

(def ^:const filter-template "(&(objectClass=%s)(%s=%s))")

(defn user-filter
  "create LDAP filter for user records from values for
   :user-object-class, :user-id-attr, and :username"
  [{:keys [user-object-class user-id-attr username]}]
  (if (not-any? str/blank? [user-object-class user-id-attr username])
    (format filter-template user-object-class user-id-attr username)
    (throw (IllegalArgumentException. "user-object-class user-id-attr and username cannot be blank"))))

(defn role-filter
  "create LDAP filter for roles for a particular user DN from 
   values for :role-object-class, :role-member-attr, and :user-dn"
  [{:keys [role-object-class role-member-attr user-dn]}]
  (if (not-any? str/blank? [role-object-class role-member-attr user-dn])
    (format filter-template role-object-class role-member-attr user-dn)
    (throw (IllegalArgumentException. "role-object-class role-member-attr and user-dn cannot be blank"))))

(defn user-dn
  [pool {:keys [user-base-dn] :as params}]
  (->> 
    (user-filter params)
    (assoc {:attributes [:dn]} :filter)
    (ldap/search pool user-base-dn)
    (map :dn)
    (first)))

(defn roles
  [pool {:keys [role-base-dn role-name-attr] :as params}]
  (->> 
    (role-filter params)
    (assoc {:attributes [role-name-attr]} :filter)
    (ldap/search pool role-base-dn)
    (map role-name-attr)
    (set)))

(defn force-bind
  [pool {:keys [password] :as params}]
  (ldap/bind? pool (user-dn pool params) password))

(defn authn-map
  [pool params]
  (->>
    (user-dn pool params)
    (assoc params :user-dn)
    (roles pool)
    (conj :eu.stratuslab.authn.handler/user)))

(defn ldap-credential-fn
  [{:keys [username password] :as p}]
  (let [params (merge ldap-defaults p)]
    (when (force-bind ldap-pool params)
      (let [authn-map (authn-map params)]
        (if-let [workflow (:cemerick.friend/workflow params)]
          (assoc authn-map :cemerick.friend/workflow workflow)
          authn-map)))))
