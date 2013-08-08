(ns eu.stratuslab.cimi.couchbase-test-utils
  (:require 
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.cb.utils :as cb-utils])
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
        db-params {:nodes [(URI. node-uri)]
                   :bucket (utils/create-uuid)
                   :bucket-pswd "pswd"}
        {:keys [nodes bucket bucket-pswd]} db-params
        mgr (ClusterManager. [(URI. mgr-uri)] "admin" "ADMIN4")]
    (try
      (.createNamedBucket mgr BucketType/COUCHBASE bucket 512 0 bucket-pswd false)
      (Thread/sleep 3000) ;; ensure bucket is loaded before running tests      
      (binding [*test-cb-client* (cb-utils/create-client db-params)]
        (try
          (f)
          (finally
            (cb-utils/shutdown *test-cb-client*))))
      (finally
        (.deleteBucket mgr bucket)))))
