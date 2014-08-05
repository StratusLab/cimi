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

(ns eu.stratuslab.cimi.resources.volume-configuration-schema-test
  (:require
    [eu.stratuslab.cimi.resources.volume-configuration :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-entry
  {:type     "http://schemas.dmtf.org/cimi/1/mapped"
   :format   "ext2"
   :capacity 1024})

(let [uri (str resource-name "/" (u/random-uuid))
      cfg (assoc valid-entry
            :acl valid-acl
            :id uri
            :resourceURI resource-uri
            :created "1964-08-25T10:00:00.0Z"
            :updated "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check VolumeConfiguration cfg))
  (expect nil? (s/check VolumeConfiguration (dissoc cfg :type)))
  (expect nil? (s/check VolumeConfiguration (dissoc cfg :format)))
  (expect (s/check VolumeConfiguration (dissoc cfg :capacity))))

(expect nil? (s/check VolumeConfigurationRef valid-entry))
(expect nil? (s/check VolumeConfigurationRef (assoc valid-entry :href "http://example.org/template")))
(expect (s/check VolumeConfigurationRef {}))
(expect nil? (s/check VolumeConfigurationRef (dissoc valid-entry :type)))
(expect nil? (s/check VolumeConfigurationRef (dissoc valid-entry :format)))
(expect nil? (s/check VolumeConfigurationRef (dissoc valid-entry :capacity)))


(run-tests [*ns*])

