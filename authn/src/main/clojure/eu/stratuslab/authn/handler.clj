(ns eu.stratuslab.authn.handler
  (:require
    [eu.stratuslab.authn.ldap :as ldap]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as credentials]
    [ring.util.response :as response]))

(def users {"user" {:username "user"
                    :password (credentials/hash-bcrypt "password")
                    :roles #{::user}}})

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/user" [] (friend/authorize #{::user} "Hello User!"))
  (friend/logout (GET "/logout" [] (response/redirect "/")))
  (GET "/login" [login_failed username]
       (str (if login_failed 
              (str "<div id='error'>Login failed.</div>"))
         "<form>"
         "<label for='username'>User</label>"
         "<input type='text' name='username' value='" username "'>"
         "<label for='password'>Password</label>"
         "<input type='text' name='password'>"
         "<input type='submit' value='login'>"
         "</form>"))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
    (friend/authenticate 
      {:workflows [(workflows/interactive-form)]
       :credential-fn (partial credentials/bcrypt-credential-fn users)
       ;;:credential-fn ldap/ldap-credential-fn
       })
    handler/site))
