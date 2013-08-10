(ns eu.stratuslab.cimi.couchbase-test-utils
  (:require 
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.utils :as utils])
  (:import [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.concurrent TimeUnit]))

(def ^:dynamic *test-cb-client* nil)

(defn temp-bucket-fixture
  "Creates a new Couchbase bucket within the server.  The server must already
   be running on the local machine and have a username/password of admin/ADMIN4.
   The bucket is removed after the tests have been run."
  [f]
  (let [mgr-uri "http://localhost:8091/"
        node-uri (str mgr-uri "pools")
        bucket (utils/create-uuid)
        password "pswd"
        cb-cfg {:uris [(URI. node-uri)]
                :bucket bucket
                :username bucket
                :password password}
        mgr (ClusterManager. [(URI. mgr-uri)] "admin" "ADMIN4")]
    (try
      (.createNamedBucket mgr BucketType/COUCHBASE bucket 512 0 password false)
      (Thread/sleep 3000) ;; ensure bucket is loaded before running tests 
      (binding [*test-cb-client* (cbc/create-client cb-cfg)]
        (try
          (f)
          (finally
            (cbc/shutdown *test-cb-client* 3000))))
      (finally
        (.deleteBucket mgr bucket)))))
