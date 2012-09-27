(ns eu.stratuslab.cimi.views.authn
  (:require [noir.core :refer [defpage pre-route]]
            [noir.response :as resp]
            [cemerick.friend :as friend]
            [net.cgrand.enlive-html :refer [deftemplate content]]
            [clojure.tools.logging :as log]))

(def context-path (atom ""))

(defn set-context-path
  [path]
  (reset! context-path path))

(defn clear-identity [response]
  (update-in response [:session] dissoc ::identity))

;; protect all but "/", "/login", and "/logout"
(pre-route [:any ["/:path" :path #"(?!login|logout|$).*"]] {}
           (friend/authenticated
            ;; no-op
            nil))

(deftemplate login "eu/stratuslab/authn/vm_rest/views/login.html" []
  [:div#wrapper :p] (content "Please login to access services."))

(defpage "/login" {}
  (apply str (login)))

(defpage "/logout" {}
  (clear-identity (resp/redirect (str @context-path "/"))))
