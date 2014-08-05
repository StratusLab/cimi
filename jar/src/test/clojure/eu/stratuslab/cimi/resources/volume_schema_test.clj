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

(ns eu.stratuslab.cimi.resources.volume-schema-test
  (:require
    [eu.stratuslab.cimi.resources.volume :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as utils]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-v-entry
  {:acl      valid-acl
   :state    "CREATING"
   :type     "http://schemas.cimi.stratuslab.eu/normal"
   :capacity 1024
   :bootable true
   :eventLog "EventLog/uuid"})

(let [volume (assoc valid-v-entry
               :id "Volume/10"
               :resourceURI resource-uri
               :created  "1964-08-25T10:00:00.0Z"
               :updated  "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check Volume volume))
  (expect nil? (s/check Volume (dissoc volume :state)))
  (expect nil? (s/check Volume (dissoc volume :bootable)))
  (expect nil? (s/check Volume (dissoc volume :eventLog)))
  (expect (s/check Volume (dissoc volume :type)))
  (expect (s/check Volume (dissoc volume :capacity))))


(run-tests [*ns*])

