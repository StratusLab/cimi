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

(ns eu.stratuslab.cimi.resources.machine-configuration-schema-test
  (:require
    [eu.stratuslab.cimi.resources.machine-configuration :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

;;
;; MachineConfiguration
;;

(def valid-mc-entry
  {:acl         valid-acl
   :name        "valid"
   :description "valid machine configuration"
   :cpu         1
   :memory      512000
   :cpuArch     "x86_64"
   :disks       [{:capacity        1024
                  :format          "ext4"
                  :initialLocation "/dev/hda"}]})

(let [disk {:capacity        1024
            :format          "ext4"
            :initialLocation "/dev/hda"}]

  (expect nil? (s/check Disk disk))
  (expect nil? (s/check Disk (dissoc disk :initialLocation)))
  (expect (s/check Disk (dissoc disk :capacity)))
  (expect (s/check Disk (dissoc disk :format)))
  (expect (s/check Disk {})))

(let [disks [{:capacity        1024
              :format          "ext4"
              :initialLocation "/dev/hda"}
             {:capacity        2048
              :format          "swap"
              :initialLocation "/dev/hdb"}]]

  (expect nil? (s/check Disks disks))
  (expect nil? (s/check Disks (rest disks)))
  (expect (s/check Disks [])))

(let [mc (assoc valid-mc-entry
           :id "/MachineConfiguration/10"
           :resourceURI type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z"
           :disks [{:capacity 1024
                    :format   "ext4"}])]

  (expect nil? (s/check MachineConfiguration mc))
  (expect nil? (s/check MachineConfiguration (dissoc mc :disks)))
  (expect (s/check MachineConfiguration (dissoc mc :cpu)))
  (expect (s/check MachineConfiguration (dissoc mc :memory)))
  (expect (s/check MachineConfiguration (dissoc mc :cpuArch))))


(run-tests [*ns*])

