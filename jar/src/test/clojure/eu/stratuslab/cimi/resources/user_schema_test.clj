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

(ns eu.stratuslab.cimi.resources.user-schema-test
  (:require
    [eu.stratuslab.cimi.resources.user :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-user-entry
  {:acl        valid-acl
   :first-name "cloud"
   :last-name  "user"
   :username   "cloud-user"})

(let [uri (uuid->uri "cloud-user")
      user (assoc valid-user-entry
             :id uri
             :resourceURI type-uri
             :created #inst "1964-08-25T10:00:00.0Z"
             :updated #inst "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check User user))
  (expect nil? (s/check User (assoc user :password "password")))
  (expect nil? (s/check User (assoc user :enabled true)))
  (expect (s/check User (assoc user :enabled "BAD")))
  (expect nil? (s/check User (assoc user :roles ["OK"])))
  (expect (s/check User (assoc user :roles [])))
  (expect (s/check User (assoc user :roles "BAD")))
  (expect nil? (s/check User (assoc user :altnames {:x500dn "certdn"})))
  (expect (s/check User (assoc user :altnames {})))
  (expect nil? (s/check User (assoc user :email "ok@example.com")))
  (expect (s/check User (dissoc user :acl)))
  (expect (s/check User (dissoc user :first-name)))
  (expect (s/check User (dissoc user :last-name)))
  (expect (s/check User (dissoc user :username))))


(run-tests [*ns*])

