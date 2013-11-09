(ns eu.stratuslab.authn.test.ldap-without-server
  (:use clojure.test
        eu.stratuslab.authn.ldap)
  (:require [clojure.string :as str]))

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

(deftest invalid-requests
  (is (nil? (valid-request? 1))) ;; argument must be a map
  (is (nil? (valid-request? {}))) ;; missing keys
  (let [request (assoc ldap-defaults :username "sluser")]
    (is (nil? (valid-request? request)))
    (is (valid-request? (assoc request :password "ok")))))

(deftest invalid-credential-fn-inputs
  (is (nil? (ldap-credential-fn {:a 1} "bad")))
  (is (nil? (ldap-credential-fn "bad" {:a 1})))
  (is (nil? (ldap-credential-fn "bad" "bad"))))
