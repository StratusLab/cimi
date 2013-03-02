(ns eu.stratuslab.cimi.db-utils
  (:require [clojure.data.json :as json])
  (:import
    [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.concurrent TimeUnit]))

(defmacro defn-db
  "This macro defines a function that wraps code accessing a couchbase
  database. The CouchbaseClient is available within the macro body as
  the 'client' local variable.  The macro adds a 'db-cfg' parameter as
  the first parameter of the generated function."
  [name & decls]
  (let [[docs v] (split-with (complement vector?) decls)
        decl-params (first v)
        db-cfg-sym 'db-cfg
        client-sym 'client
        params (vec (cons db-cfg-sym decl-params))
        body (rest v)]
    `(defn ~name ~@docs
       ~params
       (let [nodes# (:nodes ~db-cfg-sym)
             bucket# (:bucket ~db-cfg-sym)
             bucket-pswd# (:bucket-pswd ~db-cfg-sym)]
         (if-let [~client-sym (CouchbaseClient. nodes# bucket# bucket-pswd#)]
           (try
             ~@body
             (finally
               (.shutdown ~client-sym 3 TimeUnit/SECONDS))))))))

(defn-db create
  "Creates a new document in the database with the given key.  The 
   document must be a clojure map, which will be transformed to JSON
   then inserted into the database."
  [key doc]
  (let [json-data (json/write-str doc)]
    (.add client key 0 json-data PersistTo/ONE ReplicateTo/ZERO)))

(defn-db retrieve
  "Retrieve teh document associated with the given key from the 
   database.  This returns nil if the document does not exist.  The
   returned document is a map with keys transformed to keywords."
  [key]
  (if-let [doc (.get client key)]
    (json/read-str doc :key-fn keyword)))

(defn-db update
  "Updates the document associated with the given key with the 
   given document.  The document must be a clojure map, which will
   be transformed to JSON and inserted into the database."
  [key doc]
  (let [json-data (json/write-str doc)]
    (.set client key 0 json-data PersistTo/ONE ReplicateTo/ZERO)))

(defn-db delete
  "Delete the document associated with the given key from the 
   database."
  [key]
  (.delete client key))
