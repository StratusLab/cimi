(ns eu.stratuslab.cimi.cb.utils
  (:require
    [clojure.data.json :as json]
    [couchbase-clj.client :as cbc])
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
  (cbc/create-client {:uris nodes
                      :bucket bucket
                      :username bucket
                      :password bucket-pswd}))

(defn shutdown
  "Shuts down the given client.  The default timeout is 3000 ms."
  [client & [timeout-ms]]
  (let [timeout-ms (or timeout-ms 3000)]
    (cbc/shutdown client timeout-ms)))

(defn create
  "Creates a new document in the database with the given key.  The 
   document must be a clojure map, which will be transformed to JSON
   then inserted into the database."
  [cb-client key doc]
  (cbc/add-json cb-client key doc))

(defn retrieve
  "Retrieve the document associated with the given key from the 
   database.  This returns nil if the document does not exist.  The
   returned document is a map with keys transformed to keywords."
  [cb-client key]
  (cbc/get-json cb-client key))

(defn list-all
  "Provide a list of all of the database entries with the given
   resource URI."
  [cb-client resource-uri]
  ;; TODO: Provide real implementation!
  nil)

(defn update
  "Updates the document associated with the given key with the 
   given document.  The document must be a clojure map, which will
   be transformed to JSON and inserted into the database."
  [cb-client key doc]
  (cbc/set-json cb-client key doc))

(defn delete
  "Delete the document associated with the given key from the 
   database."
  [cb-client key]
  (cbc/delete cb-client key))
