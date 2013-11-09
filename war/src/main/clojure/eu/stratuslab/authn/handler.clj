(ns eu.stratuslab.authn.handler
  (:require
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [cemerick.friend :as friend]
    [ring.util.response :as response]))

(def ^{:const true
       :doc "Format string for page to be used with form-based login."}
  login-page
  "<html>
    <head>
      <title>login</title>
    </head>
    <body>
      <div id='msg'>%s</div>
      <form>
        <label for='username'>user</label>
        <input type='text' name='username' value='%s'>
        <label for='password'>password</label>
        <input type='password' name='password'>
        <input type='submit' value='login'>
      </form>
    </body>
  </html>
  ")

(defroutes authn-routes
           "Provides '/login', '/logout', and '/user' routes to support form-based
            logins.  A generic 'Not Found' error page is thrown if no route is matched.
            These routes are added automatically when using the authn-wrapper
            function."

           (GET "/user" []
                (friend/authorize #{:eu.stratuslab.authn/user} "Hello User!"))

           (friend/logout
             (GET "/logout" []
                  (response/redirect "/")))

           (GET "/login" [login_failed username]
                (let [msg (if login_failed "Login failed." "")
                      username (or username "")]
                  (format login-page msg username)))

           (route/not-found "Not Found"))

(defn authn-wrapper
  "Wraps the given application routes ('app-routes') with standard site 
  and friend middlewares.  The provided workflows will be used to configure
  friend.  The default credential function rejects all requests, so each
  workflow should be configured with its own credential function."
  [workflows app-routes]
  (let [app-handler (routes app-routes authn-routes)]
    (-> app-handler
        (friend/authenticate {:workflows workflows
                              :credential-fn (constantly nil)})
        handler/site)))
