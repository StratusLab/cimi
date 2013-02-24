(ns eu.stratuslab.authn.test.handler
  (:use clojure.test
        kerodon.test
        kerodon.core
        eu.stratuslab.authn.handler)
  (:require 
    [cemerick.friend.credentials :as credentials]
    [cemerick.friend.workflows :as workflows]))

(def users {"user" {:username "user"
                    :password (credentials/hash-bcrypt "password")
                    :roles #{:eu.stratuslab.authn.handler/user}}})

(def app 
  (let [workflows [(workflows/interactive-form
                      :credential-fn
                      (partial credentials/bcrypt-credential-fn users))]]
    (authn-wrapper workflows app-routes)))

(deftest anyone-can-view-frontpage
  (-> (session app)
    (visit "/")
    (has (text? "Hello World"))))

(deftest user-authentication
  (let [state (-> (session app)
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
  (-> (session app)
    (visit "/login")
    (fill-in "user" "user")
    (fill-in "password" "bad")
    (press "login")
    (follow-redirect)
    (within [:#msg]
      (has (text? "Login failed.")))
    (has (value? "user" "user"))))
