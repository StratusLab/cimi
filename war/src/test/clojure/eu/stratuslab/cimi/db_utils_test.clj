(ns eu.stratuslab.cimi.db-utils-test
  (:require
    [eu.stratuslab.cimi.db-utils :as db]
    [clojure.test :refer :all]
    [eu.stratuslab.cimi.couchbase-test-utils :as utils]))

(use-fixtures :once utils/temp-bucket-fixture)

(deftest test-crud-actions
  (let [key "my-document"
        doc {:hello "world"}
        udoc {:hello "universe"}]
    (db/create utils/*test-db-cfg* key doc)
    (let [rdoc (db/retrieve utils/*test-db-cfg* key)]
      (is (= doc rdoc)))
    (db/update utils/*test-db-cfg* key udoc)
    (let [rdoc (db/retrieve utils/*test-db-cfg* key)]
      (is (= udoc rdoc)))
    (db/delete utils/*test-db-cfg* key)
    (is (nil? (db/retrieve utils/*test-db-cfg* key)))))

(deftest test-crud-actions-x
  (let [key "my-document"
        doc {:hello "world"}
        udoc {:hello "universe"}]
    (db/create-x utils/*test-db-cfg* key doc)
    (let [rdoc (db/retrieve-x utils/*test-db-cfg* key)]
      (is (= doc rdoc)))
    (db/update-x utils/*test-db-cfg* key udoc)
    (let [rdoc (db/retrieve-x utils/*test-db-cfg* key)]
      (is (= udoc rdoc)))
    (db/delete-x utils/*test-db-cfg* key)
    (is (nil? (db/retrieve-x utils/*test-db-cfg* key)))))
