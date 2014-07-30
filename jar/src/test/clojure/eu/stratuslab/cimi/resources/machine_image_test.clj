;
; Copyright 2013 Centre National de la Recherche Scientifique (CNRS)
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns eu.stratuslab.cimi.resources.machine-image-test
  (:require
    [eu.stratuslab.cimi.resources.machine-image :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app routes))

(def valid-entry
  {:acl {:owner {:principal "ALPHA" :type "USER"}}
   :name "valid"
   :description "valid machine image"
   :state "CREATING"
   :type "SNAPSHOT"
   :imageLocation "http://example.com/image"
   :relatedImage {:href "MachineImage/other-uuid"}})

(deftest test-crud-workflow

  ;; anonymous post to collection should fail
  (-> (session (ring-app))
      (request base-uri :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; anonymous query to collection should also fail
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

  ;; user query to collection should succeed
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-count zero?))

  ;; user post to collection is ok
  (let [uri (-> (session (ring-app))
                (authorize "jane" "user_password")
                (request base-uri :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str "/" uri)]

    ;; read
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri)
        (t/is-status 200)
        (t/is-id uri))

    ;; update
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri
                 :request-method :put
                 :body (json/write-str {:name "OK"}))
        (t/is-status 200)
        (t/is-key-value :name "OK"))

    ;; query to ensure that resource is visible
    (let [entries (-> (session (ring-app))
                      (authorize "jane" "user_password")
                      (request base-uri)
                      (t/is-resource-uri collection-type-uri)
                      (t/is-count pos?)
                      (t/entries :machineImages))]
      (is ((set (map :id entries)) uri)))

    ;; query to ensure that resource is NOT visible to different user
    (let [entries (-> (session (ring-app))
                      (authorize "tarzan" "me,tarzan,you,jane")
                      (request base-uri)
                      (t/is-resource-uri collection-type-uri)
                      (t/is-count zero?)
                      (t/entries :machineImages))]
      (is (empty? (map :id entries))))

    ;; query to ensure that resource is visible to ::ADMIN
    (let [entries (-> (session (ring-app))
                      (authorize "root" "admin_password")
                      (request base-uri)
                      (t/is-resource-uri collection-type-uri)
                      (t/is-count pos?)
                      (t/entries :machineImages))]
      (is ((set (map :id entries)) uri)))

    ;; re-read for updated entry
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri)
        (t/is-status 200)
        (t/is-id uri)
        (t/is-key-value :name "OK"))

    ;; delete
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri
                 :request-method :delete)
        (t/is-status 200))

    ;; re-read to ensure entry is gone
    (-> (session (ring-app))
        (authorize "jane" "user_password")
        (request abs-uri)
        (t/is-status 404))))


(deftest read-non-existing-resource-fails
  (let [resource-uri (str base-uri "/" (u/random-uuid))]
    (-> (session (ring-app))
        (request resource-uri)
        (t/is-status 404))))

(deftest delete-non-existing-resource-fails
  (let [resource-uri (str base-uri "/" (u/random-uuid))]
    (-> (session (ring-app))
        (request resource-uri
                 :request-method :delete)
        (t/is-status 404))))

(deftest update-non-existing-resource-fails
  (let [resource-uri (str base-uri "/" (u/random-uuid))]
    (-> (session (ring-app))
        (request resource-uri
                 :request-method :put
                 :body (json/write-str {:name "OK"}))
        (t/is-status 404))))

(defn create-with-rest [name]
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri
               :request-method :post
               :body (json/write-str (assoc valid-entry :name name)))
      (t/location)))

(defn get-with-rest [resource-uri]
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request (str "/" resource-uri))
      (get-in [:response :body])))

(deftest test-queries

  ;; creates number of entries in database with index as name
  (let [keys (map str (range 10))
        values (map create-with-rest keys)
        m (zipmap keys values)
        bodies (map get-with-rest values)
        names (map :name bodies)]

    (is (= keys names))

    ;; ensure that all of the entries are present
    (let [docs (-> (session (ring-app))
                   (authorize "jane" "user_password")
                   (request base-uri)
                   (t/is-status 200)
                   (t/is-resource-uri collection-type-uri)
                   (t/is-id base-uri)
                   (t/is-count (partial = (count keys)))
                   (get-in [:response :body :machineImages]))]
      (is (= (set keys) (set (map :name docs)))))

    ;; limit to half the entries and make sure only a subset is returned
    (let [limit 5
          docs (-> (session (ring-app))
                   (authorize "jane" "user_password")
                   (request base-uri
                            :body (json/write-str {:limit limit}))
                   (t/is-status 200)
                   (t/is-resource-uri collection-type-uri)
                   (t/is-id base-uri)
                   (t/is-count (partial = limit))
                   (get-in [:response :body :machineImages]))]
      (is (= limit (count docs))))))

(deftest bad-methods
  (let [resource-uri (str base-uri "/" (u/random-uuid))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (do
          (-> (session (ring-app))
              (request uri
                       :request-method method
                       :body (json/write-str {:dummy "value"}))
              (t/is-status 405)))))))
