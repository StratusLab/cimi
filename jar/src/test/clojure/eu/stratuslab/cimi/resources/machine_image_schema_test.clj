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

(ns eu.stratuslab.cimi.resources.machine-image-schema-test
  (:require
    [eu.stratuslab.cimi.resources.machine-image :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-mi-entry
  {:acl           valid-acl
   :name          "valid"
   :description   "valid machine image"
   :state         "CREATING"
   :type          "SNAPSHOT"
   :imageLocation "https://image.com/myimage"
   :relatedImage  {:href "MachineImage/other-uuid"}})

(let [mi (assoc valid-mi-entry
           :id "MachineImage/10"
           :resourceURI type-uri
           :created  "1964-08-25T10:00:00.0Z"
           :updated  "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check MachineImage mi))
  (expect nil? (s/check MachineImage (dissoc mi :imageLocation)))
  (expect nil? (s/check MachineImage (dissoc mi :relatedImage)))
  (expect (s/check MachineImage (dissoc mi :state)))
  (expect (s/check MachineImage (dissoc mi :type)))
  (expect (s/check MachineImage (assoc mi :state "IMAGE"))))

(run-tests [*ns*])

