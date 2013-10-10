(ns eu.stratuslab.cimi.resources.common-test
  (:require
    [eu.stratuslab.cimi.resources.common :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [clj-schema.validation :refer [validation-errors]]
    [clojure.test :refer :all]))

(deftest test-resource-link-schema
  (let [ref {:href "https://example.org/resource"}]
    (is (empty? (validation-errors ResourceLink ref)))
    (is (not (empty? (validation-errors ResourceLink (dissoc ref :href)))))
    (is (not (empty? (validation-errors ResourceLink (assoc ref :bad "BAD")))))))

(deftest test-operation-schema
  (is (empty? (validation-errors Operation {:rel "add" :href "/add"})))
  (is (not (empty? (validation-errors Operation {:rel "add"}))))
  (is (not (empty? (validation-errors Operation {:href "/add"}))))
  (is (not (empty? (validation-errors Operation {}))))
  )

(deftest test-operations-schema
  (let [ops [{:rel "add" :href "/add"}
             {:rel "delete" :href "/delete"}]]
    (is (empty? (validation-errors Operations ops)))
    (is (empty? (validation-errors Operations (rest ops))))
    (is (not (empty? (validation-errors Operations []))))))

(deftest test-properties-schema
  (is (empty? (validation-errors Properties {"a" "b"})))
  (is (empty? (validation-errors Properties {"a" "b", "c" "d"})))
  (is (not (empty? (validation-errors Properties {})))))

(deftest test-common-attrs-schema
  (let [date "1964-08-25T10:00:00.0Z"
        minimal {:id "a"
                 :resourceURI "http://example.org/data"
                 :created date
                 :updated date}
        maximal (assoc minimal
                  :name "name"
                  :description "description"
                  :properties {"a" "b"}
                  :operations [{:rel "add" :href "/add"}])]
    (is (empty? (validation-errors CommonAttrs minimal)))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :id)))))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :resourceURI)))))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :created)))))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :updated)))))

    (is (empty? (validation-errors CommonAttrs maximal)))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :name))))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :description))))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :properties))))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :operations))))
    (is (not (empty? (validation-errors CommonAttrs (assoc maximal :bad "bad")))))
    )
  )

(deftest test-action-uri-map
  (is (= valid-actions (set (keys action-uri)))))
