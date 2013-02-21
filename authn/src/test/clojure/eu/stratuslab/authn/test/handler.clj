(ns eu.stratuslab.authn.test.handler
  (:use clojure.test
        kerodon.test
        kerodon.core
        eu.stratuslab.authn.handler))

(deftest anyone-can-view-frontpage
  (-> (session app)
    (visit "/")
    (has (text? "Hello World"))))

(deftest user-authentication
  (let [state (-> (session app)
                (visit "/user")
                (follow-redirect)
                (fill-in "User" "user")
                (fill-in "Password" "password")
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
    (fill-in "User" "user")
    (fill-in "Password" "bad")
    (press "login")
    (follow-redirect)
    (within [:#error]
      (has (text? "Login failed.")))
    (has (value? "User" "user"))))
