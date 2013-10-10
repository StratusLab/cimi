(ns eu.stratuslab.cimi.authn-workflows
  (:require
    [clojure.tools.logging :as log]
    [clojure.edn :as edn]
    [clojure.string :as s]
    [clojure-ini.core :refer [read-ini]]
    [couchbase-clj.client :as cbc]
    [compojure.handler :as handler]
    [eu.stratuslab.cimi.couchbase-cfg :refer [read-cfg]]
    [ring.middleware.format-params :refer [wrap-restful-params]]
    [eu.stratuslab.cimi.cb.bootstrap :refer [bootstrap]]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.cimi.middleware.format-response :refer [wrap-restful-response]]
    [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
    [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-servlet-paths wrap-base-uri]]
    [eu.stratuslab.cimi.routes :as routes]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]
    )
  (:import
    [java.net URI]))

(def-map-schema UserEntry
                [[:username] NonEmptyString
                 [:password] NonEmptyString
                 [:roles] (sequence-of NonEmptyString)])

(def-map-schema BasicAuthnMap
                [[(wild NonEmptyString)] UserEntry])

(def valid-basic-authn? (u/create-validation-fn BasicAuthnMap))

(defn convert-special-roles
  [s]
  (let [special-roles {"::admin" :eu.stratuslab.cimi.authn/admin
                       "::user" :eu.stratuslab.cimi.authn/user}]
    (or (get special-roles s) s)))

(defn transform-roles
  "Transforms the :roles in the user entry from a sequence of strings
   to a set of roles, where the role is the string or converted to
   a keyword if it starts with '::'."
  [m]
  (->> m
       (:roles)
       (map convert-special-roles)
       (set)
       (assoc m :roles)))

(defn basic-workflow [json-cfg]
  (if json-cfg
    (try
      (valid-basic-authn? json-cfg)
      (let [cfg (into {} (map (fn [[k v]] [k (transform-roles v)]) json-cfg))
            cred-fn (partial creds/bcrypt-credential-fn cfg)]
        (workflows/http-basic :credential-fn cred-fn))
      (catch Exception e
        (log/error "error creating basic authn workflow:" (.getMessage e))
        nil))))

(defn get-workflows [cb-client]
  [(basic-workflow (cbc/get-json cb-client "ServiceCfg/authn/basic"))])
