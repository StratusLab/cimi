(ns eu.stratuslab.cimi.cassandra-test-utils
  (:require [eu.stratuslab.cimi.resources.utils :refer :all]
            [clojure.test :refer :all]
            [clojure.tools.logging :refer [debug info warn error]]
            [clj-hector.core :refer [cluster keyspace]]
            [clj-hector.ddl :refer [add-keyspace drop-keyspace]])
  (:import [org.apache.cassandra.service EmbeddedCassandraService]
           [java.util UUID]))

(defn create-daemon
  "Creates an in-process cassandra daemon for testing.
  This daemon shuts down only when the JVM is killed,
  so care must be taken to leave the server clean
  between tests."
  []
  (println "Starting embedded cassandra service...")
  (let [svc (EmbeddedCassandraService.)]
    (.start svc)
    (println "Embedded cassandra service started.")
    svc))

;;
;; Start the server in a delay!  This avoids starting
;; the server at compile time and the various hanging
;; issues associated with that.  Unit tests should
;; explicitly dereference the daemon delay before
;; running any code that requires the cassandra
;; server.  The delay semantics will prevent multiple
;; versions of the server from starting.
;;
(defonce daemon (delay (create-daemon)))

;;
;; Parameters to create a cluster with the embedded
;; cassandra daemon. The parameters are the cluster name,
;; host (localhost), and the port.
;;
(def ^:const embedded-cluster-params
     ["Embedded Test Cluster" "127.0.0.1" 9960])

(defn embedded-cluster
  "Creates a cluster using the embedded cassandra
   daemon.  This relies on the server having been
   started elsewhere."
  []
  (apply cluster embedded-cluster-params))

(defmacro with-test-keyspace
  "Creates a test keyspace which is dropped after the
   tests are executed. (taken from clj-hector unit test)"
  [name column-family & body]
  `(let [ks-name# (.replace (str "ks" (UUID/randomUUID)) "-" "")
         cluster# (embedded-cluster)]
     (add-keyspace cluster# {:name ks-name#
                             :strategy :simple
                             :replication 1
                             :column-families [{:name ~column-family}]})
     (let [~name (keyspace cluster# ks-name#)]
       (try ~@body
            (finally (drop-keyspace cluster# ks-name#))))))

(defmacro with-test-keyspace-opts
  "Creates a test keyspace which is dropped after the
   tests are executed. (taken from clj-hector unit test)"
  [name column-family column-metadata & body]
  `(let [ks-name# (.replace (str "ks" (UUID/randomUUID)) "-" "")
         cluster# (embedded-cluster)]
     (add-keyspace cluster# {:name ks-name#
                             :strategy :simple
                             :replication 1
                             :column-families [{:name ~column-family
                                                :comparator :utf-8
                                                :validator :utf-8
                                                :column-metadata ~column-metadata}]})
     (let [~name (keyspace cluster# ks-name#)]
       (try ~@body
            (finally (drop-keyspace cluster# ks-name#))))))

(defmacro with-keyspace
  "Creates the named keyspace and the given column family.
   The keyspace if dropped after the body is evaluated."
  [ks-name column-family-name & body]
  `(let [cluster# (connect)]
     (add-keyspace cluster# {:name ~ks-name
                             :strategy :simple
                             :replication 1
                             :column-families [{:name ~column-family-name}]})
     (try
       ~@body
       (finally (drop-keyspace cluster# ~ks-name)))))

(defmacro with-embedded-server
  "Ensures that the embedded cassandra server is running
   before executing the body of the command. Currently
   no clean up of the database is done after executing
   the body."
  [& body]
  `(do
     @daemon
     (try
       ~@body
       (finally true))))

(defn start-daemon-fixture
  "Starts the embedded cassandra daemon if not already
   running."
  [f]
  (with-embedded-server
    (f)))
