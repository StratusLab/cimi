(ns eu.stratuslab.cimi.cb.utils
  (:require [clojure.data.json :as json])
  (:import
    [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.concurrent TimeUnit]))

(defn create-client
  "Creates a new instance of a Couchbase client.  Generally only one
   of these needs to be created for the application.  By default, the
   values will use the default bucket on the local Couchbase server;
   probably only useful in a testing environment."
  [{:keys [nodes bucket bucket-pswd]
    :or {:nodes [(URI/create "http://localhost:8091/pools")]
         :bucket "default"
         :bucket-pswd ""}}]
  (println nodes)
  (println bucket)
  (println bucket-pswd)
  (CouchbaseClient. nodes bucket bucket-pswd))

(defn shutdown
  "Shuts down the given client.  The default timeout is 3 seconds."
  [client & [timeout]]
  (let [timeout (or timeout 3)]
    (.shutdown client timeout TimeUnit/SECONDS)))

(defn create
  "Creates a new document in the database with the given key.  The 
   document must be a clojure map, which will be transformed to JSON
   then inserted into the database."
  [cb-client key doc]
  (let [json-data (json/write-str doc)]
    (.add cb-client key 0 json-data PersistTo/ONE ReplicateTo/ZERO)))

(defn retrieve
  "Retrieve teh document associated with the given key from the 
   database.  This returns nil if the document does not exist.  The
   returned document is a map with keys transformed to keywords."
  [cb-client key]
  (if-let [doc (.get cb-client key)]
    (json/read-str doc :key-fn keyword)))

(defn update
  "Updates the document associated with the given key with the 
   given document.  The document must be a clojure map, which will
   be transformed to JSON and inserted into the database."
  [cb-client key doc]
  (let [json-data (json/write-str doc)]
    (.set cb-client key 0 json-data PersistTo/ONE ReplicateTo/ZERO)))

(defn delete
  "Delete the document associated with the given key from the 
   database."
  [cb-client key]
  (.delete cb-client key))
