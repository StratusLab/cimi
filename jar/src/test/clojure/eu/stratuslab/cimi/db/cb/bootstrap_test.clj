(ns eu.stratuslab.cimi.db.cb.bootstrap-test
  (:require
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.db.cb.bootstrap :refer :all]
    [eu.stratuslab.cimi.db.cb.views :as views]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(deftest apply-bootstrap-to-empty-db
  (bootstrap t/*test-cb-client*)

  ;; verify that the CloudEntryPoint exists
  (let [cep (cbc/get-json t/*test-cb-client* cep/resource-name)]
    (is (not (nil? cep)))
    (is (not (empty? cep)))
    (is (= cep/resource-uri (:resourceURI cep))))

  ;; verify that views exist
  (let [user-ids-view (views/get-view t/*test-cb-client* :user-ids)
        resource-uri-view (views/get-view t/*test-cb-client* :resource-uri)
        resource-type-view (views/get-view t/*test-cb-client* :resource-type)]
    (is (not (nil? user-ids-view)))
    (is (not (nil? resource-uri-view)))
    (is (not (nil? resource-type-view))))

  ;; check that queries work
  (let [by-user-ids-q (cbq/create-query {:key          "admin"
                                         :include-docs false
                                         :stale        false})
        by-user-ids-view (views/get-view t/*test-cb-client* :user-ids)
        by-user-ids (cbc/query t/*test-cb-client* by-user-ids-view by-user-ids-q)

        by-resource-uri-q (cbq/create-query {:key          cep/resource-uri
                                             :include-docs false
                                             :stale        false})
        by-resource-uri-view (views/get-view t/*test-cb-client* :resource-uri)
        by-resource-uri (cbc/query t/*test-cb-client* by-resource-uri-view by-resource-uri-q)]
    (is (= 1 (count by-user-ids)))
    (is (= 1 (count by-resource-uri)))))

