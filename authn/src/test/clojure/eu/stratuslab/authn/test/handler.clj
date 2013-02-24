(ns eu.stratuslab.authn.test.handler
  (:use clojure.test
        kerodon.test
        kerodon.core
        eu.stratuslab.authn.handler)
  (:require 
    [compojure.core :refer :all]
    [eu.stratuslab.authn.ldap :as ldap]
    [cemerick.friend.credentials :as credentials]
    [cemerick.friend.workflows :as workflows]
    [clj-ldap.client :as ldap-client]
    [eu.stratuslab.authn.test.ldap-server :as server]))

;; will be rebound for each authentication workflow
(def ^{:dynamic true :private true} *app* nil)

;; Holds the connection to the LDAP server
(def ^:dynamic *conn* nil)

(def user-uid "user")
(def user-password "password")

(def ldap-user
  {:dn (format server/user-dn-fmt user-uid)
   :object {:objectClass "inetOrgPerson"
            :uid user-uid
            :cn "X User"
            :sn "User"
            :givenName "X"
            :mail "x@example.org"
            :userPassword user-password
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def users {user-uid {:username user-uid
                      :password (credentials/hash-bcrypt user-password)
                      :roles #{:eu.stratuslab.authn/user}}})

(defn get-ldap-params []
  {:user-object-class "inetOrgPerson"
   :user-id-attr "uid"
   :user-base-dn server/users-dn
   
   :role-base-dn server/groups-dn
   :role-object-class "groupOfUniqueNames"
   :role-member-attr "uniqueMember"
   :role-name-attr "cn"
   
   :skip-bind? false
   
   :ldap-connection-pool *conn*})

(defroutes hello-world
  (GET "/" []
    "Hello World"))

(defn get-workflow-list []
  [
   ;; using fixed map for user information
   (workflows/interactive-form
     :credential-fn
     (partial credentials/bcrypt-credential-fn users))

   ;; use authn against LDAP database
   (workflows/interactive-form
     :credential-fn
     (partial ldap/ldap-credential-fn (get-ldap-params)))
   ])

(defn- ldap-server
  "Start the LDAP server, fill data and stop it after all tests."
  [f]
  (server/start!)
  
  (binding [*conn* (ldap-client/connect {:host {:port server/ldap-port}})]
    (try
      (ldap-client/add *conn* (:dn ldap-user) (:object ldap-user))
      (catch Exception e))
    (f)
    (try
      (ldap-client/delete *conn* (:dn ldap-user))
      (catch Exception e))
    )
  
  (server/stop!))

(defn test-each-workflow
  "Loops over defined workflow and executes all tests for each one."
  [f]
  (doseq [workflow (get-workflow-list)]
    (binding [*app* (authn-wrapper [workflow] hello-world)]
      (f))))

(use-fixtures :once ldap-server)
(use-fixtures :each test-each-workflow)

(deftest anyone-can-view-frontpage
  (-> (session *app*)
    (visit "/")
    (has (text? "Hello World"))))

(deftest user-authentication
  (let [state (-> (session *app*)
                (visit "/user")
                (follow-redirect)
                (fill-in "user" user-uid)
                (fill-in "password" user-password)
                (press "login"))]
    (testing "user login"
             (-> state
               (follow-redirect)
               (has (text? "Hello User!"))))
    (testing "user logout"
             (-> state
               (visit "/logout")
               (follow-redirect)
               (visit "/user")
               (has (status? 302))))))

(deftest failed-login-shows-error
  (-> (session *app*)
    (visit "/login")
    (fill-in "user" user-uid)
    (fill-in "password" "bad")
    (press "login")
    (follow-redirect)
    (within [:#msg]
      (has (text? "Login failed.")))
    (has (value? "user" user-uid))))
