(ns eu.stratuslab.cimi.resources.webui
  (:require
    [cemerick.friend :as friend]
    [compojure.core :refer [GET POST defroutes]]
    [ring.util.response :as resp]
    [hiccup.page :as h]
    [hiccup.element :as e]
    [eu.stratuslab.cimi.resources.impl.webui-pages :refer [browser login-page authn-page]]))

(defroutes routes
           (GET "/webui" request
                (browser request))
           (GET "/login" request
                (login-page request))
           (POST "/login" request
                (resp/redirect (str (:context request) "/login?msg=login_failed")))
           (GET "/logout" request
                (friend/logout* (resp/redirect (str (:context request) "/webui"))))
           (GET "/authn" request
                (authn-page request)))
