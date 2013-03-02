(ns eu.stratuslab.cimi.resources.cloud-entry-point-test-db
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [clojure.test :refer :all]
    [eu.stratuslab.cimi.utils :as utils])
  (:import [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.concurrent TimeUnit]))

(def ^:dynamic *bucket* nil)

(defn temp-bucket-fixture
  "Creates a new Couchbase bucket within the server.  The server must already
   be running on the local machine and have a username/password of admin/ADMIN4.
   The bucket is removed after the tests have been run."
  [f]
  (let [bucket (utils/create-uuid)
        mgr-uri (URI. "http://localhost:8091/")
        mgr (ClusterManager. [mgr-uri] "admin" "ADMIN4")]
    (binding [*bucket* bucket]
      (try
        (.createNamedBucket mgr BucketType/COUCHBASE bucket 512 0 "" false)
        (Thread/sleep 3000) ;; ensure bucket is loaded before running tests
        (f)
        (finally
          (.deleteBucket mgr bucket))))))

(use-fixtures :once temp-bucket-fixture)

(deftest put-and-get-document
  (if-let [client (CouchbaseClient. [(URI. "http://localhost:8091/pools")] *bucket* "")]
    (try
      (let [input-text "hello"
            key "mykey"]
        (.set client key 0 input-text PersistTo/ONE ReplicateTo/ZERO)
        (let [retrieved-text (.get client key)]
          (is (= input-text retrieved-text)))
        (.delete client key)
        (is (nil? (.get client key))))
      (finally
        (.shutdown client 3 TimeUnit/SECONDS)))))
