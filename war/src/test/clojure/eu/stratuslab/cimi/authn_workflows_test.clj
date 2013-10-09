(ns eu.stratuslab.cimi.authn-workflows-test
  (:require
    [eu.stratuslab.cimi.authn-workflows :refer :all]
    [clojure.test :refer [deftest are is]]))

(def valid-basic-cfg {"root" {:username "root"
                              :password "admin_password"
                              :roles ["::admin" "root"]}
                      "jane" {:username "jane"
                              :password "user_password"
                              :roles ["::user"]}})

(deftest check-validation
  (let [valid valid-basic-cfg
        invalid {"root" {:password "admin_password"
                         :roles ["::admin" "root"]}}]
    (is (valid-basic-authn? valid))
    (is (thrown? Exception (valid-basic-authn? invalid)))))

(deftest check-special-roles
  (are [s correct] (= correct (convert-special-roles s))
    nil nil
    "not-special" "not-special"
    "::admin" :eu.stratuslab.cimi.authn/admin
    "::user" :eu.stratuslab.cimi.authn/user))

(deftest check-role-transform
  (let [m {:username "OK"
           :roles ["a" "::admin" "::user" "b"]}
        correct {:username "OK"
                 :roles #{"a" 
                          "b"
                          :eu.stratuslab.cimi.authn/admin
                          :eu.stratuslab.cimi.authn/user}}]
    (is (= correct (transform-roles m)))))

(deftest check-basic-workflow
  (is (basic-workflow valid-basic-cfg))
  (is (nil? (basic-workflow {"invalid" {}})))
  (is (nil? (basic-workflow nil))))


