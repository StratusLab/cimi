(ns eu.stratuslab.cimi.middleware.format-response
  "Middleware to format the response result in the format requested in
  the accept headers.  This is a small customization of the
  ring.middleware.format-response middleware to allow serialization in
  the XML format required by CIMI."
  (:require [ring.middleware.format-response :refer [make-encoder
                                                     wrap-format-response
                                                     serializable?]]
            [clj-yaml.core :as yaml]
            [cheshire.custom :as json]
            [eu.stratuslab.cimi.serial.external :as serial]))

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
