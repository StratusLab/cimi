(ns eu.stratuslab.cimi.webui.pages
  (:require
    [clojure.data.json :as json]
    [eu.stratuslab.authn.workflows.client-certificate :as cwf]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [hiccup.element :as e]))

(defn head
  [request]
  (let [context (:context request "")]
    [:head
     [:meta {:charset "utf-8"}]
     [:link {:href (str context "/js/codemirror/lib/codemirror.css") :rel "stylesheet" :type "text/css"}]
     [:link {:href (str context "/js/codemirror/addon/lint/lint.css") :rel "stylesheet" :type "text/css"}]
     [:link {:href (str context "/css/service.css") :rel "stylesheet" :type "text/css"}]
     [:script {:src (str context "/js/d3/d3.v3.min.js") :charset "utf-8"}]
     [:script {:src (str context "/js/codemirror/lib/codemirror.js") :charset "utf-8"}]
     [:script {:src (str context "/js/codemirror/mode/javascript/javascript.js") :charset "utf-8"}]
     [:script {:src (str context "/js/codemirror/addon/lint/lint.js") :charset "utf-8"}]
     [:script {:src (str context "/js/jsonlint/jsonlint.js") :charset "utf-8"}]
     [:script {:src (str context "/js/codemirror/addon/lint/json-lint.js") :charset "utf-8"}]
     [:script {:src (str context "/js/cimi-browser.js") :charset "utf-8"}]
     [:title "StratusLab CIMI"]]))

(defn user-info
  [request]
  (let [context (:context request "")]
    [:div {:class "user-info"}
     (if-let [identity (friend/current-authentication request)]
       [:span (str (:identity identity) " (") (e/link-to (str context "/logout") "logout") ")"]
       (e/link-to (str context "/login") "login"))]))

(def navigation
  [:nav])

(defn header
  [request]
  [:header
   [:div {:class "logo"}]
   (user-info request)
   navigation])

(def message
  [:section {:id "message"}])

(defn content
  [request]
  (let [context (:context request "")]
    [:main
     [:div {:id "operations"}]
     [:section {:id "metadata" :class "normal-section"}]
     [:section {:id "content" :class "normal-section"}]
     [:section {:id "acl" :class "normal-section"}]
     [:section {:id "editor-panel" :class "editor-section"}
      [:textarea {:id "editor" :rows 25}]]]))

(def footer
  [:footer
   [:p "Copyright (c) 2013 by StratusLab Contributors"]])

(defn browser
  [request]
  (h/html5
    (head request)
    [:body
     [:div {:class "wrapper"}
      (header request)
      message
      (content request)
      footer]]))

(defn login-form
  [request]
  (let [context (:context request "")
        dn (or (-> request
                   (:servlet-request)
                   (cwf/extract-client-cert-chain)
                   (cwf/extract-subject-dn))
               "")]
    [:main
     [:h1 "Login"]
     [:div {:id "operations"}
      ]
     [:section
      [:form {:id "userform" :method "POST" :action (str context "/login") :class "login"}
       [:div [:label "Username:" [:input {:type "text" :name "username"}]]]
       [:div [:label "Password:" [:input {:type "password" :name "password"}]]]
       [:div [:input {:type "submit" :class "button" :value "login"}]]]]
     [:section
      [:form {:id "certform" :method "POST" :action (str context "/login") :class "login"}
       [:div [:span "X500 DN:"] [:span dn]]
       [:div [:input {:type "submit" :class "button" :value "login"}]]]]]))

(defn login-page
  [request]
  (h/html5
    (head request)
    [:body
     [:div {:class "wrapper"}
      (header request)
      message
      (login-form request)
      footer]]))

(defn get-authn-info [request]
  (let [authn-map (friend/current-authentication request)]
    (if authn-map
      (with-out-str
        (json/pprint authn-map))
      "No authentication information in session.")))

(defn authn-page
  [request]
  (h/html5
    (head request)
    [:body
     [:div {:class "wrapper"}
      (header request)
      [:main
       [:h1 "Authentication Information"]
       [:section
        [:pre {:id "authn-info"} (get-authn-info request)]]]
      footer]]))
