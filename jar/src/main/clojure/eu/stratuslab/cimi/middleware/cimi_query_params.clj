(ns eu.stratuslab.cimi.middleware.cimi-query-params
  (:require [ring.middleware.params :refer [wrap-params]]
            [clojure.string :as str]))

(defn parse-int
  "Parses the given string as an integer.  If the value cannot be
  parsed as a base-10 integer, then the default is returned."
  [s default]
  (try
    (Integer/parseInt s)
    (catch Exception consumed
      default)))

(defn attribute-filter-fn
  "Returns a function that will filter on the attributes given in the
  list.  If the list contains '*', then a function that retains all
  attributes will be returned.  Invalid attribute names will be
  ignored."
  [^String s]
  (let [attr-names (str/split (.trim s) #"\s*,\s*")
        valid-names (set (filter #(re-matches #"(?:[a-zA-Z_]\w*)|\*" %) attr-names))]
    (if (contains? valid-names "*")
      identity
      valid-names)))

(defmulti process-query-params
          "Process the standard CIMI query parameters.  The valid parameters
          are '$first', '$last', '$filter', '$select' and '$expand'.  The
          function dispatches on the name of the query parameter after the
          leading dollar sign.  Parameters not starting with a dollar sign or
          other invalid parameters are ignored."
          (fn [req ^String key value]
            (if (.startsWith key "$")
              (keyword (.substring key 1))))
          :default nil)

(defmethod process-query-params nil
           [req key value]
  req)

(defmethod process-query-params :first
           [req key value]
  (let [index (parse-int value 1)]
    (assoc req :first index)))

(defmethod process-query-params :last
           [req key value]
  (if-let [index (parse-int value nil)]
    (assoc req :last index)
    req))

(defmethod process-query-params :select
           [req key value]
  (assoc req :select (attribute-filter-fn value)))

(defmethod process-query-params :expand
           [req key value]
  (assoc req :expand (attribute-filter-fn value)))

(defmethod process-query-params :filter
           [req key value]
  nil)

(defn wrap-cimi-query-params
  "Middleware that handles the CIMI query parameters.  This middlware
  will create a :cimi-query-params key in the request.  All of the
  query parameters are fully validated before being passed on.  This
  middleware automatically adds the wrap-params middleware; it should
  not be added separately."
  [handler & [options]]
  (wrap-params
    (fn [req]
      (let [query-params (:query-params req)]
        (reduce-kv process-query-params req query-params)))))
