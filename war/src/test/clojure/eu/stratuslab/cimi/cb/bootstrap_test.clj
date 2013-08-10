(ns eu.stratuslab.cimi.cb.bootstrap-test
  (:require
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.cb.bootstrap :refer :all]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(deftest apply-bootstrap-to-empty-db
  (bootstrap t/*test-cb-client* t/*test-cb-cfg*)
  
  ;; verify that the CloudEntryPoint exists
  (let [cep (cbc/get-json t/*test-cb-client* cep/resource-base-url)]
    (is (not (nil? cep)))
    (is (not (empty? cep)))
    (is (= cep/resource-uri (:resourceURI cep))))
 
  ;; verify that views exist
  (let [doc-id-view (cbc/get-view t/*test-cb-client* design-doc-name "doc-id")
        resource-uri-view (cbc/get-view t/*test-cb-client* design-doc-name "resource-uri")]
    (is (not (nil? doc-id-view)))
    (is (not (nil? resource-uri-view)))))
