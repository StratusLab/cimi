(ns eu.stratuslab.cimi.couchbase-test-utils
  (:require [eu.stratuslab.cimi.resources.utils :as utils])
  (:import [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.concurrent TimeUnit]))

(def ^:dynamic *test-db-cfg* nil)

(defn temp-bucket-fixture
  "Creates a new Couchbase bucket within the server.  The server must already
   be running on the local machine and have a username/password of admin/ADMIN4.
   The bucket is removed after the tests have been run."
  [f]
  (let [bucket (utils/create-uuid)
        bucket-pswd "pswd"
        nodes [(URI. "http://localhost:8091/pools")]
        test-db-cfg {:nodes nodes
                     :bucket bucket
                     :bucket-pswd bucket-pswd}

        mgr-uri (URI. "http://localhost:8091/")
        mgr (ClusterManager. [mgr-uri] "admin" "ADMIN4")]
    (binding [*test-db-cfg* test-db-cfg]
      (try
        (.createNamedBucket mgr BucketType/COUCHBASE bucket 512 0 bucket-pswd false)
        (Thread/sleep 3000) ;; ensure bucket is loaded before running tests
        (f)
        (finally
          (.deleteBucket mgr bucket))))))
