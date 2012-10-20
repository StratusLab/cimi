(ns eu.stratuslab.cimi.server
  "Entry point for running the StratusLab CIMI interface."
  (:require [clojure.tools.logging :refer [info warn]]
            [compojure.handler :as handler]
            [ring.middleware.format-params :refer [wrap-restful-params]]
          ;;  [ring.middleware.format-response :refer [wrap-restful-response]]
            [ring.middleware.format-response :refer [make-encoder wrap-format-response serializable?]]
            [clj-yaml.core :as yaml]
            [cheshire.custom :as json]
            [eu.stratuslab.cimi.routes :as routes]
            [eu.stratuslab.cimi.friend-utils :as friend-utils]
            [eu.stratuslab.cimi.serial.external :as serial]))

;; Authentication must also be configured.
;; NOTE: the context path is hardcoded!
;; FIXME: should be "/vm"
;;(friend-utils/configure-friend friend-utils/credential-fn "")

(defn wrap-servlet-paths
  "Wraps the ring handler to provide the servlet path information
  in :path-info and the context path in :context.  If this is not
  running in a servlet container, this wrapper does nothing."
  [handler]
  (fn [req]
    (let [servlet-request (:servlet-request req)
          req (if servlet-request
                (assoc req
                  :path-info (.getPathInfo servlet-request)
                  :context (.getContextPath servlet-request))
                req)]
      (handler req))))

(defn wrap-base-url
  "Wraps the ring handler to provide the base URL of the service based
  on the servlet information.  The wrapper wrap-servlet-paths should
  be called before this wrapper if the handler is running within a
  servlet container."
  [handler]
  (fn [req]
    (let [{:keys [scheme server-name server-port context]} req
          context (or context "")
          base-url (format "%s://%s:%d%s/" (name scheme) server-name server-port context)
          req (assoc req :base-url base-url)]
      (handler req))))

(defn generate-native-clojure
  [struct]
  (pr-str struct))

(defn wrap-yaml-in-html
  [body]
  (str
   "<html>\n<head></head>\n<body><div><pre>\n"
   (yaml/generate-string body)
   "</pre></div></body></html>"))

(defn wrap-restful-response
  "Wrapper that tries to do the right thing with the response :body
  and provide a solid basis for a RESTful API. It will serialize to
  JSON, YAML, Clojure or HTML-wrapped YAML depending on Accept header.
  It takes an optional :default parameter wich is an encoder-map (JSON
  by default). See wrap-format-response for more details."
  [handler & {:keys [default] :or {default (make-encoder json/generate-string
                                                         "application/json")}}]
  (wrap-format-response handler
                        :predicate serializable?
                        :encoders [(make-encoder json/generate-string
                                                 "application/json")
                                   (make-encoder yaml/generate-string
                                                 "application/x-yaml")
                                   (make-encoder generate-native-clojure
                                                 "application/clojure")
                                   (make-encoder wrap-yaml-in-html
                                                 "text/html")
                                   (make-encoder serial/resource-as-xml
                                                 "application/xml")
                                   default]
                                                :charset "utf-8"))

(def servlet-handler
  (-> (handler/site routes/main-routes)
      (wrap-base-url)
      (wrap-servlet-paths)
      (wrap-restful-params)
      (wrap-restful-response)))

(defn init
  [path]

  (info "setting context path to" path)

  (info "starting service configuration thread")

  (info "initializing authentication framework (friend)")
  #_(friend-utils/configure-friend friend-utils/credential-fn path))
