(ns eu.stratuslab.authn.ldap-with-server-test
  (:use clojure.test
        eu.stratuslab.authn.ldap)
  (:require
    [clj-ldap.client :as ldap]
    [clojure.string :as str]
    [eu.stratuslab.authn.ldap-server-test :as server]))

;; UTILITY FUNCTIONS

;; Tests are run over a variety of connection types
(def ^:dynamic *connections* nil)
(def ^:dynamic *conn* nil)

;; People to test with
(def user-x*
  {:dn (format server/user-dn-fmt "x")
   :object {:objectClass "inetOrgPerson"
            :uid "x"
            :cn "X User"
            :sn "User"
            :givenName "X"
            :mail "x@example.org"
            :userPassword "passx"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def user-y*
  {:dn (format server/user-dn-fmt "y")
   :object {:objectClass "inetOrgPerson"
            :uid "y"
            :cn "Y User"
            :sn "User"
            :givenName "Y"
            :mail "y@example.org"
            :userPassword "passy"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def user-z*
  {:dn (format server/user-dn-fmt "z")
   :object {:objectClass "inetOrgPerson"
            :uid "z"
            :cn "Z User"
            :sn "User"
            :givenName "Z"
            :mail "z@example.org"
            :userPassword "passz"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def group-x*
  {:dn (format server/group-dn-fmt "group-x")
   :object {:objectClass "groupOfUniqueNames"
            :cn "group-x"
            :uniqueMember (:dn user-x*)}})

(def group-y*
  {:dn (format server/group-dn-fmt "group-y")
   :object {:objectClass "groupOfUniqueNames"
            :cn "group-y"
            :uniqueMember (:dn user-y*)}})

(def group-xy*
  {:dn (format server/group-dn-fmt "group-xy")
   :object {:objectClass "groupOfUniqueNames"
            :cn "group-xy"
            :uniqueMember [(:dn user-x*) (:dn user-y*)]}})

(def groups
  {user-x* #{:eu.stratuslab.authn/user "group-x" "group-xy"}
   user-y* #{:eu.stratuslab.authn/user "group-y" "group-xy"}
   user-z* #{:eu.stratuslab.authn/user}})

(defn- connect-to-server
  "Opens a sequence of connection pools on the localhost server with the
   given ports"
  [port ssl-port]
  [
    (ldap/connect {:host {:port port}})
    (ldap/connect {:host {:address "localhost"
                          :port port}
                   :num-connections 4})
    (ldap/connect {:host (str "localhost:" port)})
    (ldap/connect {:ssl? true
                   :host {:port ssl-port}})
    (ldap/connect {:host {:port port}
                   :connect-timeout 1000
                   :timeout 5000})
    (ldap/connect {:host [(str "localhost:" port)
                          {:port ssl-port}]})
    (ldap/connect {:host [(str "localhost:" ssl-port)
                          {:port ssl-port}]
                   :ssl? true
                   :num-connections 5})
    ])


(defn- test-server
  "Setup server"
  [f]
  (server/start!)
  (binding [*connections* (connect-to-server server/ldap-port server/ldap-ssl-port)]
    (f))
  (server/stop!))

(defn- test-data
  "Provide test data"
  [f]
  (doseq [connection *connections*]
    (binding [*conn* connection]
      (try
        (ldap/add *conn* (:dn user-x*) (:object user-x*))
        (ldap/add *conn* (:dn user-y*) (:object user-y*))

        (ldap/add *conn* (:dn group-x*) (:object group-x*))
        (ldap/add *conn* (:dn group-y*) (:object group-y*))
        (ldap/add *conn* (:dn group-xy*) (:object group-xy*))

        (catch Exception e))
      (f)
      (try
        (ldap/delete *conn* (:dn user-x*))
        (ldap/delete *conn* (:dn user-y*))

        (ldap/delete *conn* (:dn group-x*))
        (ldap/delete *conn* (:dn group-y*))
        (ldap/delete *conn* (:dn group-xy*))

        (catch Exception e)))))

(use-fixtures :each test-data)
(use-fixtures :once test-server)

;; TESTS BEGIN HERE!

(deftest get-user-dn
  (doall
    (for [user [user-x* user-y*]]
      (let [params {:user-base-dn server/users-dn
                    :user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :username (get-in user [:object :uid])}]
        (is (= (:dn user) (user-dn *conn* params)))))))

(deftest nil-dn-for-unknown-user
  (let [user user-z*
        params {:user-object-class "inetOrgPerson"
                :user-id-attr "uid"
                :user-base-dn server/users-dn
                :username (get-in user [:object :uid])}]
    (is (nil? (user-dn *conn* params)))))

(deftest exception-for-bad-user-dn-request
  (let [user user-x*
        params {:user-object-class "inetOrgPerson"
                :user-id-attr "uid"
                :user-base-dn server/users-dn
                ;; removed! :username (get-in user [:object :uid])
                }]
    (is (thrown? IllegalArgumentException (user-dn *conn* params)))))

(deftest get-roles
  (doall
    (for [user [user-x* user-y* user-z*]]
      (let [params {:role-base-dn server/groups-dn
                    :role-object-class "groupOfUniqueNames"
                    :role-member-attr "uniqueMember"
                    :role-name-attr "cn"
                    :user-dn (:dn user)}]
        (is (= (get groups user) (roles *conn* params)))))))

(deftest check-force-bind
  (doall
    (for [user [user-x* user-y*]]
      (let [params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn server/users-dn
                    :username (get-in user [:object :uid])
                    :password (get-in user [:object :userPassword])}
            bad-params (assoc params :password "bad")]
        (is (force-bind *conn* params))
        (is (not (force-bind *conn* bad-params)))
        ))))

(deftest check-force-bind-nonexistant-user
  (doall
    (for [user [user-z*]]
      (let [params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn server/users-dn
                    :username (get-in user [:object :uid])
                    :password (get-in user [:object :userPassword])}]
        (is (not (force-bind *conn* params)))))))

(deftest get-authn-map
  (doall
    (for [user [user-x* user-y*]]
      (let [username (get-in user [:object :uid])
            params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn server/users-dn

                    :role-base-dn server/groups-dn
                    :role-object-class "groupOfUniqueNames"
                    :role-member-attr "uniqueMember"
                    :role-name-attr "cn"

                    :username username
                    :cemerick.friend/workflow :form}

            correct {:identity username
                     :roles (get groups user)
                     :cemerick.friend/workflow :form}]
        (is (= correct (authn-map *conn* params)))))))

(deftest nil-authn-map-for-missing-user
  (doall
    (for [user [user-z*]]
      (let [username (get-in user [:object :uid])
            params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn server/users-dn

                    :role-base-dn server/groups-dn
                    :role-object-class "groupOfUniqueNames"
                    :role-member-attr "uniqueMember"
                    :role-name-attr "cn"

                    :username username
                    :cemerick.friend/workflow :form}]
        (is (nil? (authn-map *conn* params)))))))

(deftest valid-credentials-with-bind
  (doall
    (for [user [user-x* user-y*]]
      (let [username (get-in user [:object :uid])
            password (get-in user [:object :userPassword])
            ldap-params {:user-object-class "inetOrgPerson"
                         :user-id-attr "uid"
                         :user-base-dn server/users-dn

                         :role-base-dn server/groups-dn
                         :role-object-class "groupOfUniqueNames"
                         :role-member-attr "uniqueMember"
                         :role-name-attr "cn"

                         :skip-bind? false

                         :ldap-connection-pool *conn*}

            cred-map {:username username
                      :password password
                      :cemerick.friend/workflow :form}

            correct {:identity username
                     :roles (get groups user)
                     :cemerick.friend/workflow :form}]
        (is (= correct (ldap-credential-fn ldap-params cred-map)))))))

(deftest valid-credentials-without-bind
  (doall
    (for [user [user-x* user-y*]]
      (let [username (get-in user [:object :uid])
            ldap-params {:user-object-class "inetOrgPerson"
                         :user-id-attr "uid"
                         :user-base-dn server/users-dn

                         :role-base-dn server/groups-dn
                         :role-object-class "groupOfUniqueNames"
                         :role-member-attr "uniqueMember"
                         :role-name-attr "cn"

                         :skip-bind? true

                         :ldap-connection-pool *conn*}

            cred-map {:username username
                      :cemerick.friend/workflow :form}

            correct {:identity username
                     :roles (get groups user)
                     :cemerick.friend/workflow :form}]
        (is (= correct (ldap-credential-fn ldap-params cred-map)))))))
