(ns eu.stratuslab.authn.test.ldap
  (:use clojure.test
        eu.stratuslab.authn.ldap)
  (:require
    [clj-ldap.client :as ldap]
    [clojure.string :as str]
    [eu.stratuslab.authn.test.ldap-server :as server])
  (:import [com.unboundid.ldap.sdk LDAPException]))

(deftest correct-user-filter
  (let [params {:user-object-class "a"
                :user-id-attr "b"
                :username "c"}
        result (user-filter params)]
    (is (= result "(&(objectClass=a)(b=c))"))))

(deftest correct-role-filter
  (let [params {:role-object-class "a"
                :role-member-attr "b"
                :user-dn "c"}
        result (role-filter params)]
    (is (= result "(&(objectClass=a)(b=c))"))))

(deftest invalid-user-filter-combinations
  (let [values ["a" "b" "c" "" " " nil]]
    (doall
      (for [a values b values c values]
        (let [params {:user-object-class a
                      :user-id-attr b
                      :username c}]
          (if (some str/blank? [a b c])
            (is (thrown? IllegalArgumentException (user-filter params)))))))))

(deftest missing-user-filter-parameters
  (let [values ["a" nil]]
    (doall
      (for [a values b values c values]
        (let [params {:user-object-class a
                      :user-id-attr b
                      :username c}
              params (into {} (filter (fn [[k v]] v) params))]
          (if (not= 3 (count params))
            (is (thrown? IllegalArgumentException (user-filter params)))
            (is (user-filter params))))))))

(deftest invalid-role-filter-combinations
  (let [values ["a" "b" "c" "" " " nil]]
    (doall
      (for [a values b values c values]
        (let [params {:role-object-class a
                      :role-member-attr b
                      :user-dn c}]
          (if (some str/blank? [a b c])
            (is (thrown? IllegalArgumentException (role-filter params)))))))))

(deftest missing-role-filter-parameters
  (let [values ["a" nil]]
    (doall
      (for [a values b values c values]
        (let [params {:role-object-class a
                      :role-member-attr b
                      :user-dn c}
              params (into {} (filter (fn [[k v]] v) params))]
          (if (not= 3 (count params))
            (is (thrown? IllegalArgumentException (user-filter params)))
            (is (role-filter params))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Tests are run over a variety of connection types
(def port* 1389)
(def ssl-port* 1636)
(def ^:dynamic *connections* nil)
(def ^:dynamic *conn* nil)

;; Tests concentrate on a single object class
(def root* "dc=alienscience,dc=org,dc=uk")
(def base* (str "ou=people," root*))
(def dn*  (str "cn=%s," base*))
(def user-base* (str "ou=users," root*))
(def role-base* (str "ou=groups," root*))
(def user-dn* (str "uid=%s," user-base*))
(def role-dn* (str "cn=%s," role-base*))
(def object-class* #{"top" "person"})

;; Variable to catch side effects
(def ^:dynamic *side-effects* nil)

;; Result of a successful write
(def success*      {:code 0 :name "success"})

;; People to test with
(def person-a*
     {:dn (format dn* "testa")
      :object {:objectClass object-class*
               :cn "testa"
               :sn "a"
               :description "description a"
               :telephoneNumber "000000001"
               :userPassword "passa"}})

(def person-b*
     {:dn (format dn* "testb")
      :object {:objectClass object-class*
               :cn "testb"
               :sn "b"
               :description "description b"
               :telephoneNumber ["000000002" "00000003"]
               :userPassword "passb"}})

(def person-c*
     {:dn (format dn* "testc")
      :object {:objectClass object-class*
               :cn "testc"
               :sn "c"
               :description "description c"
               :telephoneNumber "000000004"
               :userPassword "passc"}})

(def user-x*
  {:dn (format user-dn* "x")
   :object {:objectClass "inetOrgPerson"
            :uid "x"
            :cn "X User"
            :sn "User"
            :givenName "X"
            :mail "x@example.org"
            :userPassword "passx"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def user-y*
  {:dn (format user-dn* "y")
   :object {:objectClass "inetOrgPerson"
            :uid "y"
            :cn "Y User"
            :sn "User"
            :givenName "Y"
            :mail "y@example.org"
            :userPassword "passy"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def user-z*
  {:dn (format user-dn* "z")
   :object {:objectClass "inetOrgPerson"
            :uid "z"
            :cn "Z User"
            :sn "User"
            :givenName "Z"
            :mail "z@example.org"
            :userPassword "passz"
            :seeAlso "cn=Charles Loomis,ou=LAL,o=CNRS,c=FR,o=GRID-FR"}})

(def group-x*
  {:dn (format role-dn* "group-x")
   :object {:objectClass "groupOfUniqueNames"
            :cn "group-x"
            :uniqueMember (:dn user-x*)}})

(def group-y*
  {:dn (format role-dn* "group-y")
   :object {:objectClass "groupOfUniqueNames"
            :cn "group-y"
            :uniqueMember (:dn user-y*)}})

(def group-xy*
  {:dn (format role-dn* "group-xy")
   :object {:objectClass "groupOfUniqueNames"
            :cn "group-xy"
            :uniqueMember [(:dn user-x*) (:dn user-y*)]}})

(def groups
  {user-x* #{"group-x" "group-xy"}
   user-y* #{"group-y" "group-xy"}
   user-z* #{}})

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
  (server/start! port* ssl-port*)
  (binding [*connections* (connect-to-server port* ssl-port*)]
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

(deftest get-user-dn
  (doall
    (for [user [user-x* user-y*]]
      (let [params {:user-base-dn user-base*
                    :user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :username (get-in user [:object :uid])}]
        (is (= (:dn user) (user-dn *conn* params)))))))

(deftest nil-dn-for-unknown-user
  (let [user user-z*
        params {:user-object-class "inetOrgPerson"
                :user-id-attr "uid"
                :user-base-dn user-base*
                :username (get-in user [:object :uid])}]
    (is (nil? (user-dn *conn* params)))))

(deftest exception-for-bad-user-dn-request
  (let [user user-x*
        params {:user-object-class "inetOrgPerson"
                :user-id-attr "uid"
                :user-base-dn user-base*
                ;; removed! :username (get-in user [:object :uid])
                }]
    (is (thrown? IllegalArgumentException (user-dn *conn* params)))))

(deftest get-roles
  (doall
    (for [user [user-x* user-y* user-z*]]
      (let [params {:role-base-dn role-base*
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
                    :user-base-dn user-base*
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
                    :user-base-dn user-base*
                    :username (get-in user [:object :uid])
                    :password (get-in user [:object :userPassword])}]
        (is (not (force-bind *conn* params)))))))

(deftest get-authn-map
  (doall
    (for [user [user-x* user-y*]]
      (let [username (get-in user [:object :uid])
            params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn user-base*
                    
                    :role-base-dn role-base*
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
                    :user-base-dn user-base*
                    
                    :role-base-dn role-base*
                    :role-object-class "groupOfUniqueNames"
                    :role-member-attr "uniqueMember"
                    :role-name-attr "cn"
                    
                    :username username
                    :cemerick.friend/workflow :form}]
        (is (nil? (authn-map *conn* params)))))))

(deftest invalid-requests
  (is (nil? (valid-request? 1))) ;; argument must be a map
  (is (nil? (valid-request? {}))) ;; missing keys
  (let [request (into {} (map (fn [k] [k false]) required-request-keys))]
    (is (nil? (valid-request? request)))
    (is (valid-request? (assoc request :password "ok")))))

(deftest invalid-credential-map
  (is (nil? (ldap-credential-fn "bad"))))

(deftest valid-credentials-with-bind
  (doall
    (for [user [user-x* user-y*]]
      (let [username (get-in user [:object :uid])
            password (get-in user [:object :userPassword])
            params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn user-base*
                    
                    :role-base-dn role-base*
                    :role-object-class "groupOfUniqueNames"
                    :role-member-attr "uniqueMember"
                    :role-name-attr "cn"
                    
                    :username username
                    :password password

                    :cemerick.friend/workflow :form
                    
                    :skip-bind? false
                    
                    :ldap-connection-pool *conn*}

            correct {:identity username
                     :roles (get groups user)
                     :cemerick.friend/workflow :form}]
        (is (= correct (ldap-credential-fn params)))))))

(deftest valid-credentials-without-bind
  (doall
    (for [user [user-x* user-y*]]
      (let [username (get-in user [:object :uid])
            params {:user-object-class "inetOrgPerson"
                    :user-id-attr "uid"
                    :user-base-dn user-base*
                    
                    :role-base-dn role-base*
                    :role-object-class "groupOfUniqueNames"
                    :role-member-attr "uniqueMember"
                    :role-name-attr "cn"
                    
                    :username username

                    :cemerick.friend/workflow :form
                    
                    :skip-bind? true
                    
                    :ldap-connection-pool *conn*}

            correct {:identity username
                     :roles (get groups user)
                     :cemerick.friend/workflow :form}]
        (is (= correct (ldap-credential-fn params)))))))
