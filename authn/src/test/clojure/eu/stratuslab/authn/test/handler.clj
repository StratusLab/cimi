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

(def users {"user" {:username "user"
                    :password (credentials/hash-bcrypt "password")
                    :roles #{:eu.stratuslab.authn.handler/user}}})

;; Holds the connection to the LDAP server
(def ^:dynamic *conn* nil)

(def user*
  {:dn (format server/user-dn-fmt "x")
   :object {:objectClass "inetOrgPerson"
            :uid "x"
            :cn "X User"
            :sn "User"
            :givenName "X"
            :mail "x@example.org"
            :userPassword "passx"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def users {"user" {:username "user"
                    :password (credentials/hash-bcrypt "password")
                    :roles #{:eu.stratuslab.authn.handler/user}}})


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

(def workflow-list 
  [
   ;; using fixed map for user information
   (workflows/interactive-form
     :credential-fn
     (partial credentials/bcrypt-credential-fn users))

   ;; use authn against LDAP database
   ;;(workflows/interactive-form
   ;;  :credential-fn
   ;;  (partial ldap/ldap-credential-fn (get-ldap-params)))
   ])

(defn- ldap-server
  "Start the LDAP server, fill data and stop it after all tests."
  [f]
  (server/start!)
  
  (binding [*conn* (ldap-client/connect {:host {:port server/ldap-port}})]
    (try
      (ldap-client/add *conn* (:dn user*) (:object user*))
      (catch Exception e))
    (f)
    (try
      (ldap-client/delete *conn* (:dn user*))
      (catch Exception e))
    )
  
  (server/stop!))

(defn test-each-workflow
  "Loops over defined workflow and executes all tests for each one."
  [f]
  (doseq [workflow workflow-list]
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
                (fill-in "user" "user")
                (fill-in "password" "password")
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
    (fill-in "user" "user")
    (fill-in "password" "bad")
    (press "login")
    (follow-redirect)
    (within [:#msg]
      (has (text? "Login failed.")))
    (has (value? "user" "user"))))
