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

(ns eu.stratuslab.cimi.resources.job-schema-test
  (:require
    [eu.stratuslab.cimi.resources.job :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

;;
;; Job
;;

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-job-entry
  {:acl                valid-acl
   :state              "QUEUED"
   :targetResource     "Machine/uuid-1"
   :affectedResources  ["Machine/uuid-2"]
   :action             "http://schemas.cimi.stratuslab.eu/create-volume"
   :returnCode         0
   :progress           0
   :statusMessage      "none"
   :timeOfStatusChange  "2013-08-25T10:00:00.00Z"
   :parentJob          "Job/uuid-1"
   :nestedJobs         ["Job/uuid-2"]})

(let [job (assoc valid-job-entry
            :id "/Job/10"
            :resourceURI resource-uri
            :created  "1964-08-25T10:00:00.0Z"
            :updated  "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check Job job))
  (expect nil? (s/check Job (dissoc job :state)))
  (expect nil? (s/check Job (dissoc job :affectedResources)))
  (expect nil? (s/check Job (dissoc job :returnCode)))
  (expect nil? (s/check Job (dissoc job :progress)))
  (expect nil? (s/check Job (dissoc job :statusMessage)))
  (expect nil? (s/check Job (dissoc job :timeOfStatusChange)))
  (expect nil? (s/check Job (dissoc job :parentJob)))
  (expect nil? (s/check Job (dissoc job :nestedJobs)))
  (expect (s/check Job (dissoc job :targetResource)))
  (expect (s/check Job (dissoc job :action))))


(run-tests [*ns*])

