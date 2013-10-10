(ns eu.stratuslab.cimi.couchbase-cfg
  (:require
    [clojure.string :as s]
    [clojure-ini.core :refer [read-ini]]
    [clojure.tools.logging :as log])
  (:import
    [java.net URI]))

(defn hostport->uri
  "Creates a Couchbase URI from an argument of the form host:port.  The
   port part is optional and if not specified will default to 8091. If
   the argument does not match this pattern then nil is returned."
  [hostport]
  (let [hostport (or hostport "")
        [host port] (next (re-matches #"^([^:]+)(?::(\d+))?$" hostport))
        port (or port "8091")]
    (if host
      (URI/create (str "http://" host ":" port "/pools"))
      (do
        (log/error "invalid host:port specification:" hostport)
        nil))))

(defn hostports->uris
  "Converts the string containing host:port values separated by 
   whitespace into a list of of Couchbase URIs.  If the argument
   is nil, then a list with a single URI pointing to the localhost
   will be returned."
  [hostports]
  (if hostports
    (->> (s/split hostports #"\s+")
         (remove empty?)
         (map hostport->uri)
         (remove nil?))
    [(URI/create "http://localhost:8091/pools")]))

(defn get-value
  "Retrieves a value from the map containing the ini configuration
   file information.  This will look for values in the following
   sections in order: :cimi, :DEFAULT, and unnamed.  If the value
   is found, the default is returned."
  [ini-map name & [default]]
  (or
    (get-in ini-map [:cimi name])
    (get-in ini-map [:DEFAULT name])
    (get-in ini-map [name])
    default))

(defn read-cfg
  "The argument (usually a file name) will be read as an ini file.
   The information will transformed into a map containing Couchbase
   connection parameters.

   Any errors will be logged and will result in an empty map being
   returned."
  [cfg]
  (try
    (let [ini-map (read-ini cfg :keywordize? true :comment-char \#)
          host (get-value ini-map :host)
          bucket (get-value ini-map :bucket "default")
          username (get-value ini-map :username "")
          password (get-value ini-map :password "")]
      {:uris (hostports->uris host)
       :bucket bucket
       :username username
       :password password})
    (catch Exception e
      (log/error "error reading" cfg ": " (.getMessage e)))))
