(ns eu.stratuslab.cimi.cb.utils-test
  (:require
    [eu.stratuslab.cimi.cb.utils :refer :all]
    [clojure.test :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as utils]))

(use-fixtures :once utils/temp-bucket-fixture)

(deftest test-crud-actions
  (let [key "my-document"
        doc {:hello "world"}
        udoc {:hello "universe"}]
    (create utils/*test-db-cfg* key doc)
    (let [rdoc (retrieve utils/*test-db-cfg* key)]
      (is (= doc rdoc)))
    (update utils/*test-db-cfg* key udoc)
    (let [rdoc (retrieve utils/*test-db-cfg* key)]
      (is (= udoc rdoc)))
    (delete utils/*test-db-cfg* key)
    (is (nil? (retrieve utils/*test-db-cfg* key)))))
