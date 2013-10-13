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

(ns eu.stratuslab.cimi.resources.event-test
  (:require
    [eu.stratuslab.cimi.resources.event :refer :all]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [clj-schema.validation :refer [validation-errors]]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app routes))

(def valid-entry
  {:acl {:owner {:principal "::ADMIN" :type "ROLE"}}
   :name "title"
   :description "description"
   :timestamp "20131031T10:00:00.00Z"
   :type "unknown"
   :outcome "Unknown"
   :severity "critical"})

(deftest lifecycle

  ;; anonymous create should fail
  (-> (session (ring-app))
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; user create should also fail
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri
               :request-method :post
               :body (json/write-str valid-entry))
      (t/is-status 403))

  ;; anonymous query should fail
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

  ;; user query should succeed
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri collection-type-uri)
      (t/is-count zero?))

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (authorize "root" "admin_password")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str "/" uri)]

    (is uri)

    ;; verify that the new entry is accessible
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri)
        (t/is-status 200)
        (t/does-body-contain valid-entry))

    ;; query to see that entry is listed
    (let [entries (-> (session (ring-app))
                      (authorize "root" "admin_password")
                      (request base-uri)
                      (t/is-status 200)
                      (t/is-resource-uri collection-type-uri)
                      (t/is-count pos?)
                      (t/entries :events))]
      (is ((set (map :id entries)) uri)))

    ;; update entry with new title
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :put
                 :body (json/write-str {:name "new title"}))
        (t/is-status 200))

    ;; check that update was done
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri)
        (t/is-status 200)
        (t/is-key-value :name "new title")
        (t/is-key-value :description "description"))

    ;; delete the entry
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :delete)
        (t/is-status 200))

    ;; ensure that it really is gone
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri)
        (t/is-status 404))))

(deftest bad-methods
  (let [resource-uri (str base-uri "/" (u/create-uuid))]
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
