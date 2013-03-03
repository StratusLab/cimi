(ns eu.stratuslab.cimi.middleware.cfg-params-test
  (:require
    [eu.stratuslab.cimi.middleware.cfg-params :refer :all]
    [clojure.test :refer [deftest is are]]
    [cemerick.friend.credentials :as creds])
  (:import [java.net URI]))

(deftest check-default-values
  (let [tfunc (fn [req]
                (is (= default-db-cfg (db-cfg req)))
                (is (= {} (admin-authn-map req))))]
    ((wrap-cfg-params tfunc) {})))

(deftest check-with-db-cfg
  (let [test-db-cfg {:nodes [(URI/create "http://db.example.org:8091/pools")]
                     :bucket "my-bucket"
                     :bucket-pswd "my-pswd"}
        tfunc (fn [req]
                (is (= test-db-cfg (db-cfg req)))
                (is (= {} (admin-authn-map req))))]
    ((wrap-cfg-params test-db-cfg tfunc) {})))

(deftest check-with-admin-creds
  (let [test-db-cfg {:nodes [(URI/create "http://db.example.org:8091/pools")]
                     :bucket "my-bucket"
                     :bucket-pswd "my-pswd"}
        test-admin-user "my-admin"
        test-admin-pswd "my-passwd"
        tfunc (fn [req]
                (let [authn-map (admin-authn-map req)
                      hash (get-in authn-map [test-admin-user :password])
                      authn-map (update-in authn-map [test-admin-user :password] (fn [x] nil))]
                  (is (= test-db-cfg (db-cfg req)))
                  (is (=
                        {test-admin-user {:identity test-admin-user
                                          :username test-admin-user
                                          :password nil}}
                        authn-map))
                  (is (creds/bcrypt-verify test-admin-pswd hash))))]
    ((wrap-cfg-params test-db-cfg test-admin-user test-admin-pswd tfunc) {})))
