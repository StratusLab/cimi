(ns eu.stratuslab.cimi.webui.routes
  (:require
    [cemerick.friend :as friend]
    [compojure.core :refer [GET defroutes]]
    [ring.util.response :as resp]
    [hiccup.page :as h]
    [hiccup.element :as e]
    [eu.stratuslab.cimi.webui.pages :refer [browser login-page]]))

(defroutes routes
           (GET "/webui" request
                (browser request))
           (GET "/login" request
                (login-page request))
           (POST "/login" request
                (resp/redirect (str (:context request) "/login?msg=login_failed")))
           (GET "/logout" request
                (friend/logout* (resp/redirect (str (:context request) "/webui")))))
