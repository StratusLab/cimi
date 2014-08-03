(ns eu.stratuslab.cimi.resources.impl.webui-pages
  (:require
    [clojure.data.json :as json]
    [eu.stratuslab.authn.workflows.client-certificate :as cwf]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [hiccup.element :as e]))

(def head
  [:head
   [:meta {:charset "utf-8"}]
   [:link {:href "/cimi/static/js/codemirror/lib/codemirror.css" :rel "stylesheet" :type "text/css"}]
   [:link {:href "/cimi/static/js/codemirror/addon/lint/lint.css" :rel "stylesheet" :type "text/css"}]
   [:link {:href "/cimi/static/css/bootstrap.min.css" :rel "stylesheet" :type "text/css"}]
   [:link {:href "/cimi/static/css/service.css" :rel "stylesheet" :type "text/css"}]
   [:title "StratusLab CIMI"]])

(defn user-info
  [request]

  (if-let [identity (:identity (friend/current-authentication request))]
    [:div {:class "pull-right"}
     [:button {:class "btn btn-primary"}
      (e/link-to "/cimi/User/" identity)]
     [:button {:class "btn btn-primary"}
      (e/link-to "/cimi/logout" "logout")]]
    [:div {:class "pull-right"}
     [:button {:class "btn btn-primary"}
      (e/link-to "/cimi/login" "login")]]))

(defn user-nav-info
  [request]

  (if-let [identity (:identity (friend/current-authentication request))]
    [:li {:class "dropdown"}
     [:a {:href "#" :class "dropdown-toggle" :data-toggle "dropdown"}
      identity
      [:span {:class "caret"}]]
     [:ul {:class "dropdown-menu" :role "menu"}
      [:li (e/link-to (str "#User/" identity) "profile")]
      [:li (e/link-to "/cimi/authn" "authn. info")]
      [:li {:class "divider"}]
      [:li (e/link-to "/cimi/logout" "logout")]]]
    [:li (e/link-to "/cimi/login" "login")]))

(def breadcrumbs
  [:nav {:id "breadcrumbs"}])

#_(defn header
  [request]
  [:header
   (user-info request)
   [:div {:class "logo"}]])

(defn header
  [request]
  [:header
   [:nav {:class "navbar navbar-inverse" :role "navigation"}
    [:div {:class "container-fluid"}
     [:div {:class "navbar-header"}
      [:button {:type        "button" :class "navbar-toggle"
                :data-toggle "collapse" :data-target "#top-navbar"}
       [:span {:class "sr-only"} "Toggle navigation"]
       [:span {:class "icon-bar"}]
       [:span {:class "icon-bar"}]
       [:span {:class "icon-bar"}]]
      [:div {:class "logo"}]]

     [:div {:class "collapse navbar-collapse" :id "#top-navbar"}
      [:ul {:class "nav navbar-nav navbar-right"}
       (user-nav-info request)]]]]])

(def dialog
  [:div {:class "modal fade" :id "error-dialog"
         :tabindex "-1" :role "dialog"
         :aria-labelledby "error-dialog-title" :aria-hidden "true"}
   [:div {:class "modal-dialog"}
    [:div {:class "modal-content"}
     [:div {:class "modal-header"}
      [:button {:type "button" :class "close" :data-dismiss "modal"}
       [:span {:aria-hidden "true"} "&times;"]
       [:span {:class "sr-only"} "Close"]]
      [:h4 {:class "modal-title" :id "error-dialog-title"}]]
     [:div {:class "modal-body" :id "error-dialog-body"}]]]])

(def content
  [:main
   [:div {:id "operations" :class "pull-right"}]
   [:h1
    [:span {:id "resource-title"}]
    [:span {:class "badge" :id "collection-count"}]]
   [:section {:id "metadata" :class "normal-section"}]
   [:section {:id "content" :class "normal-section"}]
   [:section {:id "acl" :class "normal-section"}]
   [:section {:id "editor-panel" :class "editor-section"}
    [:textarea {:id "editor" :rows 25}]]])

(def footer
  [:footer
   [:p "Copyright &copy; 2013-2014 by StratusLab Contributors"]])

(def scripts
  [:div
   [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"}]
   [:script {:src "/cimi/static/js/bootstrap.min.js"}]
   [:script {:src "/cimi/static/js/d3/d3.v3.min.js" :charset "utf-8"}]
   [:script {:src "/cimi/static/js/codemirror/lib/codemirror.js" :charset "utf-8"}]
   [:script {:src "/cimi/static/js/codemirror/mode/javascript/javascript.js" :charset "utf-8"}]
   [:script {:src "/cimi/static/js/codemirror/addon/lint/lint.js" :charset "utf-8"}]
   [:script {:src "/cimi/static/js/jsonlint/jsonlint.js" :charset "utf-8"}]
   [:script {:src "/cimi/static/js/codemirror/addon/lint/json-lint.js" :charset "utf-8"}]
   [:script {:src "/cimi/static/js/cimi-browser.js" :charset "utf-8"}]])

(defn login-form
  [request]
  (let [dn (or (-> request
                   (:servlet-request)
                   (cwf/extract-client-cert-chain)
                   (cwf/extract-subject-dn))
               "")]
    [:main
     [:h1 "Login"]
     [:div {:id "operations"}]
     [:section {:class "col-md-4"}
      [:form {:id "userform" :method "POST" :action "/cimi/login" :class "form-signin" :role "form"}
       [:h2 {:class "form-signin-heading"} "Please sign in"]
       [:input {:type "text" :name "username" :class "form-control" :placeholder "username" :required "" :autofocus ""}]
       [:input {:type "password" :name "password" :class "form-control" :placeholder "password"}]
       [:button {:class "btn btn-lg btn-primary btn-block" :type "submit"} "Sign in"]
       ]]
     [:section {:class "col-md-4"}
      [:form {:id "certform" :method "POST" :action "/cimi/login" :class "form-signin" :role "form"}
       [:h2 {:class "form-signin-heading"} (str "Certificate DN: " dn)]
       [:button {:class "btn btn-lg btn-primary btn-block" :type "submit"} "Sign in with certificate"]
       ]]]))

(defn get-authn-info [request]
  (let [authn-map (friend/current-authentication request)]
    (if authn-map
      (with-out-str
        (json/pprint authn-map))
      "No authentication information in session.")))

(defn page-skeleton
  [request contents]
  (h/html5
    head
    [:body
     [:div {:class "container"}
      (header request)
      breadcrumbs
      contents
      footer]
     dialog
     scripts]))

(defn browser-page
  [request]
  (page-skeleton request content))

(defn login-page
  [request]
  (page-skeleton request (login-form request)))

(defn authn-page
  [request]
  (let [contents [:main
                  [:h1 "Authentication Information"]
                  [:section
                   [:pre {:id "authn-info"} (get-authn-info request)]]]]
    (page-skeleton request contents)))
