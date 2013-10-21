(ns eu.stratuslab.cimi.authn-workflows-test
  (:require
    [eu.stratuslab.cimi.authn-workflows :refer :all]
    [clojure.test :refer :all]))

(def valid-basic-cfg {"root" {:username "root"
                              :password "admin_password"
                              :roles ["::ADMIN" "root"]}
                      "jane" {:username "jane"
                              :password "user_password"
                              :roles ["::USER"]}})

(deftest check-validation
  (let [valid valid-basic-cfg
        invalid {"root" {:password "admin_password"
                         :roles ["::ADMIN" "root"]}}]
    (is (valid-basic-authn? valid))
    (is (thrown? Exception (valid-basic-authn? invalid)))))

(deftest check-basic-workflow
  (is (basic-workflow valid-basic-cfg))
  (is (nil? (basic-workflow {"invalid" {}})))
  (is (nil? (basic-workflow nil))))


