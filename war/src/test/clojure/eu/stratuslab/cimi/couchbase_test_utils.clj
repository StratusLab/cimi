(ns eu.stratuslab.cimi.couchbase-test-utils
  (:require 
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.cb.bootstrap :refer [bootstrap]]
    [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
    [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-base-uri]]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds])
  (:import
    [java.net URI]
    [com.couchbase.client ClusterManager CouchbaseClient]
    [com.couchbase.client.clustermanager BucketType]
    [net.spy.memcached PersistTo ReplicateTo]
    [java.util.logging Logger Level]
    [java.util.concurrent TimeUnit]))

(def ^:dynamic *test-cb-client* nil)

(def test-users {"root" {:username "root"
                         :password (creds/hash-bcrypt "admin_password")
                         :roles #{:eu.stratuslab.cimi.authn/admin}}
                 "jane" {:username "jane"
                         :password (creds/hash-bcrypt "user_password")
                         :roles #{:eu.stratuslab.cimi.authn/user}}})

(defn make-ring-app [resource-routes]
  (-> resource-routes
    (friend/authenticate {:allow-anon? true
                          :unauthenticated-handler #(workflows/http-basic-deny "StratusLab " %)
                          :realm "StratusLab"
                          :credential-fn #(creds/bcrypt-credential-fn test-users %)
                          :workflows [(workflows/http-basic)]})
    (wrap-cb-client *test-cb-client*)
    (wrap-base-uri)))

(defn set-cb-logging []
  (System/setProperty "net.spy.log.LoggerImpl" "net.spy.memcached.compat.log.SunLogger")
  (.setLevel Level/WARNING (Logger/getLogger "net.spy.memcached"))
  (.setLevel Level/WARNING (Logger/getLogger "com.couchbase.client"))
  (.setLevel Level/WARNING (Logger/getLogger "com.couchbase.client.vbucket"))
  (let [logger (Logger/getLogger "")
        handlers (seq (.getHandlers (.getParent logger)))]
    (.setLevel logger Level/WARNING)
    (doall (map #(.setLevel % Level/WARNING) handlers))))

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
      #_(set-cb-logging)  ;; seems to cause failures on newest Couchbase version
      (Thread/sleep 2000) ;; ensure bucket is loaded before running tests 
      (binding [*test-cb-client* (cbc/create-client cb-cfg)]
        (try
          (bootstrap *test-cb-client*)
          (f)
          (finally
            (cbc/shutdown *test-cb-client* 3000))))
      (finally
        (.deleteBucket mgr bucket)))))
