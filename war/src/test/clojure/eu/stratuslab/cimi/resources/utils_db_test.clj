(ns eu.stratuslab.cimi.resources.utils-db-test
  (:require
   [eu.stratuslab.cimi.resources.utils :refer :all]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [clojure.test :refer :all]
   [couchbase-clj.client :as cbc]))

(use-fixtures :each t/temp-bucket-fixture)

(deftest check-get-resource
  (let [uri "DummyResource/10"
        resource {:name "name" :description "check-get-resource"}]
    (cbc/add-json t/*test-cb-client* uri resource)
    (is (= resource (get-resource t/*test-cb-client* uri)))
    (is (thrown? Exception (get-resource t/*test-cb-client* "BAD URI")))))

(deftest check-resolve-href-identities
  (are [x] (= x (resolve-href t/*test-cb-client* x))
       1
       "a"
       [1 2 3]
       {}
       {:name "name"}
       {:name "name" :properties {"a" 1 "b" 2} :data [1 2 3 4 5]}))

(deftest check-resolve-href
  (let [data1 {:name "BAD" :alpha "A" :beta "B"}]
    (cbc/add-json t/*test-cb-client* "Data/1" data1)

    (are [x correct] (= correct (resolve-href t/*test-cb-client* x))
         {:href "Data/1"} {:alpha "A" :beta "B"}
         {:href "Data/1" :name "BAD"} {:alpha "A" :beta "B"}
         {:href "Data/1" :alpha "OK"} {:alpha "OK" :beta "B"}
         {:href "Data/1" :alpha "OK" :beta "OK"} {:alpha "OK" :beta "OK"}
         ))
  (is (thrown? Exception (resolve-href t/*test-cb-client* {:href "Data/BAD"}))))

(deftest check-resolve-hrefs
  (let [data1 {:name "BAD" :value "BAD" :other "OK"}
        data2 {:four {:href "Data/3"} :two 2 :value "BAD"}
        data3 {:three 3 :name "BAD"}
        data4 {:name "4"
               :alpha {:href "Data/1" :value "OK"}
               :beta {:href "Data/2" :value "OK" :name "BAD"}}
        correct {:name "4"
                 :alpha {:value "OK" :other "OK"}
                 :beta {:two 2 :four {:three 3} :value "OK"}}]
    (cbc/add-json t/*test-cb-client* "Data/1" data1)
    (cbc/add-json t/*test-cb-client* "Data/2" data2)
    (cbc/add-json t/*test-cb-client* "Data/3" data3)
    (cbc/add-json t/*test-cb-client* "Data/4" data4)
    (is (= correct (resolve-hrefs t/*test-cb-client* data4)))))