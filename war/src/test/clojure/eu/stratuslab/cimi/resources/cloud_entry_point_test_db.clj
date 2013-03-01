(ns eu.stratuslab.cimi.resources.cloud-entry-point-test-db
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [clojure.test :refer :all]
    [eu.stratuslab.cimi.utils :as utils]
    [com.ashafa.clutch :as clutch]))

(def ^:dynamic *db-url* nil)

(defn with-temp-bucket
  "Creates a new CouchDB/Couchbase bucket within the server.  The
   server must already be running on the local machine.  The bucket
   is removed after the tests have been run."
  [f]
  (let [bucket-name (utils/create-uuid)
        bucket-name "default"
        db-url (str "http://localhost:8092/" bucket-name)]
    (binding [*db-url* db-url]
      (try
        (clutch/create-database db-url)
        (f)
        (catch Exception e
          (.printStackTrace e))
        (finally
          true
          #_(clutch/delete-database db-url))))))

(use-fixtures :once with-temp-bucket)

;; one of these will fail and print the database url
(deftest check-db-url
  (is (nil? *db-url*))
  (is (not (nil? *db-url*))))

(deftest check-put-document
  (clutch/put-document *db-url* {:_id "my-test-doc" :hi "cal"}))
