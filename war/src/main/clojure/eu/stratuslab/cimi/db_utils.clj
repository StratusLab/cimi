(ns eu.stratuslab.cimi.db-utils
  (:require [clojure.data.json :as json])
  (:import
    [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.concurrent TimeUnit]))

(defmacro defn-db
  "This macro defines a function that wraps code accessing a cassandra
  database. Two function signatures are defined: one as given in the
  declaration and a second with an explicit keyspace prepended to the
  parameters list.  The first declaration calls the second with the
  default keyspace.  The body of the function is wrapped in a
  try/catch that logs the error and re-throws the exception."
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

(defn create [db-cfg key doc]
  (let [{:keys [nodes bucket bucket-pswd]} db-cfg]
    (if-let [client (CouchbaseClient. nodes bucket bucket-pswd)]
      (try
        (let [json-data (json/write-str doc)]
          (.add client key 0 json-data PersistTo/ONE ReplicateTo/ZERO))
        (finally
          (.shutdown client 3 TimeUnit/SECONDS))))))

(defn-db create-x [key doc]
  (let [json-data (json/write-str doc)]
    (.add client key 0 json-data PersistTo/ONE ReplicateTo/ZERO)))

(defn retrieve [db-cfg key]
  (let [{:keys [nodes bucket bucket-pswd]} db-cfg]
    (if-let [client (CouchbaseClient. nodes bucket bucket-pswd)]
      (try
        (if-let [doc (.get client key)]
          (json/read-str doc :key-fn keyword))
        (finally
          (.shutdown client 3 TimeUnit/SECONDS))))))

(defn-db retrieve-x [key]
  (if-let [doc (.get client key)]
    (json/read-str doc :key-fn keyword)))

(defn update [db-cfg key doc]
  (let [{:keys [nodes bucket bucket-pswd]} db-cfg]
    (if-let [client (CouchbaseClient. nodes bucket bucket-pswd)]
      (try
        (let [json-data (json/write-str doc)]
          (.set client key 0 json-data PersistTo/ONE ReplicateTo/ZERO))
        (finally
          (.shutdown client 3 TimeUnit/SECONDS))))))

(defn-db update-x [key doc]
  (let [json-data (json/write-str doc)]
    (.set client key 0 json-data PersistTo/ONE ReplicateTo/ZERO)))

(defn delete [db-cfg key]
  (let [{:keys [nodes bucket bucket-pswd]} db-cfg]
    (if-let [client (CouchbaseClient. nodes bucket bucket-pswd)]
      (try
        (.delete client key)
        (finally
          (.shutdown client 3 TimeUnit/SECONDS))))))

(defn-db delete-x [key]
  (.delete client key))
