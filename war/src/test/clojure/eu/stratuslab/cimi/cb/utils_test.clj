(ns eu.stratuslab.cimi.cb.utils-test
  (:require
    [eu.stratuslab.cimi.cb.utils :refer :all]
    [clojure.test :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as utils]))

(use-fixtures :once utils/temp-bucket-fixture)

(deftest test-crud-workflow
  (let [key "my-document"
        doc {:hello "world"}
        udoc {:hello "universe"}]

    ;; create a document
    (create utils/*test-cb-client* key doc)

    ;; verify that it exists
    (let [rdoc (retrieve utils/*test-cb-client* key)]
      (is (= doc rdoc)))

    ;; update the document
    (update utils/*test-cb-client* key udoc)

    ;; verify that the document has been updated
    (let [rdoc (retrieve utils/*test-cb-client* key)]
      (is (= udoc rdoc)))

    ;; delete the document
    (delete utils/*test-cb-client* key)

    ;; ensure that the document is no longer available
    (is (nil? (retrieve utils/*test-cb-client* key)))))
