(ns eu.stratuslab.cimi.resources.utils
  "General utilities for dealing with resources."
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clj-time.core :as time]
    [clj-time.format :as time-fmt]
    [clj-schema.validation :refer [validation-errors]])
  (:import [java.util UUID Date]))

(defn create-uuid
  "Provides the string representation of a pseudo-random UUID."
  []
  (str (UUID/randomUUID)))

(defn strip-service-attrs
  "Strips common attributes from the map whose values are controlled
   entirely by the service.  These include :id, :created, :updated, 
   :resourceURI, and :operations."
  [m]
  (dissoc m :id :created :updated :resourceURI :operations))

(defn set-time-attributes
  "Sets the updated and created attributes in the request.  If the
  existing? is nil/false, then the created attribute it set;
  otherwise, it is removed from the request."
  [data]
  (let [updated (time-fmt/unparse (:date-time time-fmt/formatters) (time/now))
        created (or (:created data) updated)]
    (assoc data :created created :updated updated)))

(defn body->json
  "Converts the contents of body (that must be something readable) into
   a clojure datastructure.  If the body is empty, then an empty map is
   returned."
  [body]
  (if body
    (json/read (io/reader body) :key-fn keyword :eof-error false :eof-value {})
    {}))

(defn create-validation-fn
  "Creates a validation function that compares a resource against the
   given schema.  The generated function raises an exception with the 
   violations of the schema or the resource itself if everything's OK."
  [schema]
  (fn [resource]
    (let [errors (validation-errors schema resource)]
      (if (empty? errors)
        resource
        (throw (Exception. (str "resource does not satisfy defined schema\n"
                             (str/join "\n" errors))))))))
