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

(ns eu.stratuslab.cimi.resources.event-schema-test
  (:require
    [eu.stratuslab.cimi.resources.event :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-state-content
  {:resName  "resName"
   :resource "resource"
   :resType  "resType"
   :state    "pending"
   :previous "pending"})

(def valid-alarm-content
  {:resName  "resName"
   :resource "resource"
   :resType  "resType"
   :code     "141"
   :detail   "detail"})

(def valid-model-content
  {:resName  "resName"
   :resource "resource"
   :resType  "resType"
   :change   "change"
   :detail   "detail"})

(def valid-access-content
  {:operation "operation"
   :resource  "resource"
   :detail    "detail"
   :initiator "initiator"})

(def valid-cadf-content
  {:key       "value"
   :timestamp "1964-08-25T00:00:00.000Z"})

(expect nil? (s/check StateContent valid-state-content))
(expect (s/check StateContent (dissoc valid-state-content :resName)))
(expect (s/check StateContent (dissoc valid-state-content :resource)))
(expect (s/check StateContent (dissoc valid-state-content :resType)))
(expect (s/check StateContent (dissoc valid-state-content :state)))
(expect nil? (s/check StateContent (dissoc valid-state-content :previous)))

(expect nil? (s/check AlarmContent valid-alarm-content))
(expect (s/check AlarmContent (dissoc valid-alarm-content :resName)))
(expect (s/check AlarmContent (dissoc valid-alarm-content :resource)))
(expect (s/check AlarmContent (dissoc valid-alarm-content :resType)))
(expect (s/check AlarmContent (dissoc valid-alarm-content :code)))
(expect nil? (s/check AlarmContent (dissoc valid-alarm-content :detail)))

(expect nil? (s/check ModelContent valid-model-content))
(expect (s/check ModelContent (dissoc valid-model-content :resName)))
(expect (s/check ModelContent (dissoc valid-model-content :resource)))
(expect (s/check ModelContent (dissoc valid-model-content :resType)))
(expect (s/check ModelContent (dissoc valid-model-content :change)))
(expect nil? (s/check ModelContent (dissoc valid-model-content :detail)))

(expect nil? (s/check AccessContent valid-access-content))
(expect (s/check AccessContent (dissoc valid-access-content :operation)))
(expect (s/check AccessContent (dissoc valid-access-content :resource)))
(expect nil? (s/check AccessContent (dissoc valid-access-content :detail)))
(expect (s/check AccessContent (dissoc valid-access-content :initiator)))

(expect nil? (s/check CADFContent valid-cadf-content))
(expect nil? (s/check CADFContent (dissoc valid-access-content :key)))
(expect (s/check CADFContent {}))

(def valid-entry
  {:timestamp "1964-08-25T00:00:00.000Z"
   :outcome   "Pending"
   :severity  "low"
   :contact   "admin@example.com"
   :type      "state"
   :content   valid-state-content})

(let [uri (str resource-name "/" (u/random-uuid))
      event (assoc valid-entry
              :id uri
              :acl valid-acl
              :resourceURI resource-uri
              :created "1964-08-25T10:00:00.0Z"
              :updated "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check Event event))
  (expect (s/check Event (dissoc event :timestamp)))
  (expect (s/check Event (dissoc event :outcome)))
  (expect (s/check Event (dissoc event :severity)))
  (expect nil? (s/check Event (dissoc event :contact)))
  (expect (s/check Event (dissoc event :type)))
  (expect (s/check Event (dissoc event :content)))
  )



(run-tests [*ns*])

