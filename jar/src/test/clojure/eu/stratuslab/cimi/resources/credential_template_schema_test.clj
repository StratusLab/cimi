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

(ns eu.stratuslab.cimi.resources.credential-template-schema-test
  (:require
    [eu.stratuslab.cimi.resources.credential-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-up-cred
  {:username "myusername"
   :password "mypassword"})

(def valid-sshkey-cred
  {:publicKey "DUMMY KEY SHOULD BE BASE64"})

(expect nil? (s/check UsernamePasswordCredential valid-up-cred))
(expect (s/check UsernamePasswordCredential (dissoc valid-up-cred :username)))
(expect (s/check UsernamePasswordCredential (dissoc valid-up-cred :password)))
(expect (s/check UsernamePasswordCredential (assoc valid-up-cred :invalid "BAD")))

(expect nil? (s/check SSHPublicKeyCredential valid-sshkey-cred))
(expect (s/check SSHPublicKeyCredential (dissoc valid-sshkey-cred :publicKey)))
(expect (s/check SSHPublicKeyCredential (assoc valid-sshkey-cred :invalid "BAD")))

(let [uri (str resource-name "/" (u/random-uuid))
      tpl (assoc valid-up-cred
            :id uri
            :acl valid-acl
            :resourceURI resource-uri
            :created "1964-08-25T10:00:00.0Z"
            :updated "1964-08-25T10:00:00.0Z")]

  (println (s/check CredentialTemplate tpl))
  (expect nil? (s/check CredentialTemplate tpl))
  (expect (s/check CredentialTemplate (assoc tpl :invalid "BAD"))))

(let [uri (str resource-name "/" (u/random-uuid))
      tpl (assoc valid-sshkey-cred
            :id uri
            :acl valid-acl
            :resourceURI resource-uri
            :created "1964-08-25T10:00:00.0Z"
            :updated "1964-08-25T10:00:00.0Z")]

  (println (s/check CredentialTemplate tpl))
  (expect nil? (s/check CredentialTemplate tpl))
  (expect (s/check CredentialTemplate (assoc tpl :invalid "BAD"))))


(run-tests [*ns*])

