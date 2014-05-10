(ns eu.stratuslab.cimi.server-test
  (:require
    [eu.stratuslab.cimi.routes :as routes]
    [cemerick.friend :as friend]
    [eu.stratuslab.cimi.couchbase-cfg :refer :all]
    [clojure.test :refer [deftest are is]]
    [ring.util.response :as resp]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [hiccup.page :as h]
    [hiccup.element :as e]
    ;;[ring.adapter.jetty]
    [cemerick.friend.credentials :refer (hash-bcrypt)]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds]))
  (:import
    [java.net URI]
    [java.io StringReader]))

(deftest dummy-test
  (is (= 1 1)))

(def users (atom {"friend"       {:username "friend"
                                  :password (hash-bcrypt "clojure")
                                  :pin      "1234" ;; only used by multi-factor
                                  :roles    #{::user}}
                  "friend-admin" {:username "friend-admin"
                                  :password (hash-bcrypt "clojure")
                                  :pin      "1234" ;; only used by multi-factor
                                  :roles    #{::admin}}}))

(derive ::admin ::user)

(handler/site
  (friend/authenticate
    (routes/get-main-routes)
    {:allow-anon?          true
     :login-uri            "/cimi/login"
     :default-landing-uri  "/cimi/webui"
     :unauthorized-handler #(-> (h/html5 [:h2 "You do not have sufficient privileges to access " (:uri %)])
                                resp/response
                                (resp/status 401))
     :credential-fn        #(creds/bcrypt-credential-fn @users %)
     :workflows            [(workflows/interactive-form)]}))
