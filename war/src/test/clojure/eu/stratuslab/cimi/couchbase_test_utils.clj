(ns eu.stratuslab.cimi.couchbase-test-utils
  (:require
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.cb.bootstrap :refer [bootstrap]]
    [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
    [eu.stratuslab.cimi.middleware.servlet-request :refer [wrap-base-uri]]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.credentials :as creds]
    [clojure.test :refer [is]]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.logging :as log])
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
                         :roles #{"::ADMIN"}}
                 "jane" {:username "jane"
                         :password (creds/hash-bcrypt "user_password")
                         :roles #{"test-role"}}})

(defn is-status [m status]
  (is (= status (get-in m [:response :status])))
  m)

(defn is-key-value [m k v]
  (is (= v (get-in m [:response :body k])))
  m)

(defn is-resource-uri [m type-uri]
  (is-key-value m :resourceURI type-uri))

(defn is-operation-present [m op]
  (let [operations (get-in m [:response :body :operations])
        op (some #(= op %) (map :rel operations))]
    (is op))
  m)

(defn is-operation-absent [m op]
  (let [operations (get-in m [:response :body :operations])
        op (some #(= op %) (map :rel operations))]
    (is (nil? op)))
  m)

(defn is-id [m id]
  (is-key-value m :id id))

(defn is-count [m f]
  (let [count (get-in m [:response :body :count])]
    (is (f count))
    m))

(defn is-nil-response [m]
  (is (nil? (:response m)))
  m)

(defn dump [m]
  (pprint m)
  m)

(defn does-body-contain [m v]
  (let [body (get-in m [:response :body])]
    (is (= (merge body v) body))))

(defn has-job [m]
  (let [job-uri (get-in m [:response :headers "CIMI-Job-URI"])]
    (is job-uri)
    (is (.startsWith job-uri "Job/")))
  m)

(defn location [m]
  (let [uri (get-in m [:response :headers "Location"])]
    (is uri)
    uri))

(defn entries [m k]
  (get-in m [:response :body k]))

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

(defn flush-bucket-fixture
  [f]
  (try
    (f)
    (finally
      (if (not (.get (.flush (cbc/get-client *test-cb-client*))))
        (log/warn "flush of couchbase bucket failed")))))

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
      (.createNamedBucket mgr BucketType/COUCHBASE bucket 512 0 password true)
      #_(set-cb-logging) ;; seems to cause failures on newest Couchbase version
      (Thread/sleep 2000) ;; ensure bucket is loaded before running tests 
      (binding [*test-cb-client* (cbc/create-client cb-cfg)]
        (try
          (bootstrap *test-cb-client*)
          (f)
          (finally
            (if (not (cbc/shutdown *test-cb-client* 2000))
              (log/warn "shutdown of couchbase client failed")))))
      (finally
        (.deleteBucket mgr bucket)))))
