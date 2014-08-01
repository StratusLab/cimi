(ns eu.stratuslab.cimi.resources.webui
  (:require
    [cemerick.friend :as friend]
    [compojure.core :refer [GET POST defroutes]]
    [ring.util.response :as resp]
    [hiccup.page :as h]
    [hiccup.element :as e]
    [eu.stratuslab.cimi.resources.impl.webui-pages :refer [browser-page login-page authn-page]]))

(defroutes routes
           (GET "/cimi/webui" request
                (browser-page request))
           (GET "/cimi/login" request
                (login-page request))
           (POST "/cimi/login" request
                (resp/redirect "/cimi/login?msg=login_failed"))
           (GET "/cimi/logout" request
                (friend/logout* (resp/redirect  "/cimi/webui")))
           (GET "/cimi/authn" request
                (authn-page request)))
