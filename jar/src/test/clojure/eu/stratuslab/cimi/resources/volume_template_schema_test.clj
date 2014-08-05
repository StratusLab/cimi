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

(ns eu.stratuslab.cimi.resources.volume-template-schema-test
  (:require
    [eu.stratuslab.cimi.resources.volume-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

;;
;; VolumeTemplate
;;

(def valid-vt-entry
  {:volumeConfig     {:href "VolumeConfiguration/uuid"}
   :volumeImage      {:href "VolumeImage/uuid"}
   :meterTemplates   [{:href "MeterTemplate/uuid"}]
   :eventLogTemplate {:href "EventLogTemplate/uuid"}})

(let [uri (str resource-name "/" (u/random-uuid))
      vt (assoc valid-vt-entry
           :acl valid-acl
           :id uri
           :resourceURI resource-uri
           :created "1964-08-25T10:00:00.0Z"
           :updated "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check VolumeTemplate vt))
  (expect (s/check VolumeTemplate (dissoc vt :volumeConfig)))
  (expect nil? (s/check VolumeTemplate (dissoc vt :volumeImage)))
  (expect nil? (s/check VolumeTemplate (dissoc vt :meterTemplate)))
  (expect nil? (s/check VolumeTemplate (dissoc vt :eventLogTemplate))))

(expect nil? (s/check VolumeTemplateRef valid-vt-entry))
(expect nil? (s/check VolumeTemplateRef (dissoc valid-vt-entry :volumeConfig)))
(expect nil? (s/check VolumeTemplateRef (dissoc valid-vt-entry :volumeImage)))
(expect nil? (s/check VolumeTemplateRef (dissoc valid-vt-entry :meterTemplate)))
(expect nil? (s/check VolumeTemplateRef (dissoc valid-vt-entry :eventLogTemplate)))
(expect (s/check VolumeTemplateRef {}))


(run-tests [*ns*])

