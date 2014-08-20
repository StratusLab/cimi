(ns eu.stratuslab.cimi.db.cb.bootstrap-test
  (:require
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.db.cb.bootstrap :refer :all]
    [eu.stratuslab.cimi.db.cb.views :as views]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]))

(use-fixtures :once t/temp-bucket-fixture)

(deftest check-bootstrap-adds-views

  ;; the bootstrap mechanism should be called automatically
  ;; when the temporary bucket is created; ensure that this
  ;; has setup the views

  ;; verify that views exist
  (let [user-ids-view (views/get-view t/*test-cb-client* :user-ids)
        resource-uri-view (views/get-view t/*test-cb-client* :resource-uri)
        resource-type-view (views/get-view t/*test-cb-client* :resource-type)]
    (is (not (nil? user-ids-view)))
    (is (not (nil? resource-uri-view)))
    (is (not (nil? resource-type-view)))))

