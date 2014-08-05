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

(ns eu.stratuslab.cimi.resources.user-template-schema-test
  (:require
    [eu.stratuslab.cimi.resources.user-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-user-template-entry
  {:acl                valid-acl
   :user-type          "username-password"
   :first-name         "cloud"
   :last-name          "user"
   :username           "cloud-user"
   :email              "user@example.com"
   :password-cleartext "alpha"
   :password-confirm   "alpha"})

(let [uri (uuid->id "cloud-user")
      user (assoc valid-user-template-entry
             :id uri
             :resourceURI resource-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check UserTemplate user))
  (expect (s/check UserTemplate (dissoc user :acl "password")))
  (expect (s/check UserTemplate (dissoc user :user-type "password")))
  (expect (s/check UserTemplate (dissoc user :first-name "password")))
  (expect (s/check UserTemplate (dissoc user :last-name "password")))
  (expect (s/check UserTemplate (dissoc user :username "password")))
  (expect (s/check UserTemplate (dissoc user :email "password")))
  (expect (s/check UserTemplate (dissoc user :password-cleartext "password")))
  (expect (s/check UserTemplate (dissoc user :password-confirm "password")))
  (expect (s/check UserTemplate (assoc user :enabled "BAD"))))


(run-tests [*ns*])

