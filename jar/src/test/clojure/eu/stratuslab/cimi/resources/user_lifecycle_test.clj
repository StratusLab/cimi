;
; Copyright 2014 Centre National de la Recherche Scientifique (CNRS)
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
(ns eu.stratuslab.cimi.resources.user-lifecycle-test
  (:require
    [eu.stratuslab.cimi.resources.user :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.couchbase-test-utils :as t]
    [eu.stratuslab.cimi.app.routes :as routes]
    [ring.util.response :as rresp]
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [eu.stratuslab.cimi.resources.common.schema :as c]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def ^:const base-uri (str c/service-context resource-name))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "sluser"
                         :type      "USER"
                         :right     "VIEW"}]})

(def valid-entry
  {:acl        valid-acl
   :first-name "StratusLab"
   :last-name  "User"
   :username   "sluser"
   :email      "sluser@example.com"})

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

  ;; anonymous query should NOT succeed
  (-> (session (ring-app))
      (request base-uri)
      (t/is-status 403))

  ;; user query should succeed
  (-> (session (ring-app))
      (authorize "jane" "user_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri collection-uri)
      (t/is-count zero?))

  ;; admin query should succeed
  (-> (session (ring-app))
      (authorize "root" "admin_password")
      (request base-uri)
      (t/is-status 200)
      (t/is-resource-uri collection-uri))                   ;; Don't check count; may not be zero because of admin user.

  ;; add a new entry
  (let [uri (-> (session (ring-app))
                (authorize "root" "admin_password")
                (request base-uri
                         :request-method :post
                         :body (json/write-str valid-entry))
                (t/is-status 201)
                (t/location))
        abs-uri (str c/service-context uri)]

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
                      (t/is-resource-uri collection-uri)
                      (t/is-count pos?)
                      (t/entries :users))]
      (is ((set (map :id entries)) uri)))

    ;; update entry with new last name
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri
                 :request-method :put
                 :body (json/write-str {:last-name "User2"}))
        (t/is-status 200))

    ;; check that update was done
    (-> (session (ring-app))
        (authorize "root" "admin_password")
        (request abs-uri)
        (t/is-status 200)
        (t/is-key-value :first-name "StratusLab")
        (t/is-key-value :last-name "User2"))

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
