(ns eu.stratuslab.cimi.cb.bootstrap-test
  (:require
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.cb.bootstrap :refer :all]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(deftest apply-bootstrap-to-empty-db
  (bootstrap t/*test-cb-client*)
  
  ;; verify that the CloudEntryPoint exists
  (let [cep (cbc/get-json t/*test-cb-client* cep/resource-base-url)]
    (is (not (nil? cep)))
    (is (not (empty? cep)))
    (is (= cep/resource-uri (:resourceURI cep))))
 
  ;; verify that views exist
  (let [doc-id-view (cbc/get-view t/*test-cb-client* design-doc-name "doc-id")
        resource-uri-view (cbc/get-view t/*test-cb-client* design-doc-name "resource-uri")]
    (is (not (nil? doc-id-view)))
    (is (not (nil? resource-uri-view))))

  ;; check that queries work
  (let [by-doc-id-q (cbq/create-query {:key cep/resource-base-url :include-docs false})
        by-doc-id (cbc/query t/*test-cb-client* design-doc-name "doc-id" by-doc-id-q)
     	by-resource-uri-q (cbq/create-query {:key cep/resource-uri :include-docs false})
        by-resource-uri (cbc/query t/*test-cb-client* design-doc-name "resource-uri" by-resource-uri-q)]
    (println by-doc-id)
    (println by-resource-uri)
    (is (= 1 (count by-doc-id)))
    (is (= 1 (count by-resource-uri)))))

