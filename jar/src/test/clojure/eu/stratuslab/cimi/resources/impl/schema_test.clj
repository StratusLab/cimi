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

(ns eu.stratuslab.cimi.resources.impl.schema-test
  (:require
    [eu.stratuslab.cimi.resources.impl.schema :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as utils]
    [eu.stratuslab.cimi.resources.user :as user]
    [eu.stratuslab.cimi.resources.service-configuration :as sc]
    [eu.stratuslab.cimi.resources.volume-configuration :as vc]
    [eu.stratuslab.cimi.resources.volume-image :as vi]
    [eu.stratuslab.cimi.resources.volume-template :as vt]
    [eu.stratuslab.cimi.resources.volume :as v]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.resources.machine-configuration :as mc]
    [eu.stratuslab.cimi.resources.machine-image :as mi]
    [eu.stratuslab.cimi.resources.service-message :as sm]

    [schema.core :as s]
    [clj-time.core :as time]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "me" :type "USER"}})

;;
;; User
;;

(def valid-user-entry
  {:acl        valid-acl
   :first-name "cloud"
   :last-name  "user"
   :username   "cloud-user"})

(let [uri (user/uuid->uri "cloud-user")
      user (assoc valid-user-entry
             :id uri
             :resourceURI user/type-uri
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

;;
;; Service configuration
;;

(def valid-sc-entry
  {:acl      valid-acl
   :service  "authn"
   :instance "first"})

(let [uri (sc/uuid->uri "authn.first")
      sc (assoc valid-sc-entry
           :id uri
           :resourceURI user/type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check ServiceConfiguration sc))
  (expect nil? (s/check ServiceConfiguration (dissoc sc :instance)))
  (expect (s/check ServiceConfiguration (assoc sc :bad "BAD"))))

;;
;; VolumeConfiguration
;;

(def valid-vc-entry
  {:acl      valid-acl
   :type     "http://stratuslab.eu/cimi/1/raw"
   :format   "ext4"
   :capacity 1000})

(let [uri (vc/uuid->uri (utils/create-uuid))
      vc (assoc valid-vc-entry
           :id uri
           :resourceURI vc/type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check VolumeConfiguration vc))
  (expect nil? (s/check VolumeConfiguration (dissoc vc :type)))
  (expect nil? (s/check VolumeConfiguration (dissoc vc :format)))
  (expect (s/check VolumeConfiguration (dissoc vc :capacity))))

;;
;; VolumeImage
;;

(def valid-vi-entry
  {:acl           valid-acl
   :state         "CREATING"
   :imageLocation {:href "GWE_nifKGCcXiFk42XaLrS8LQ-J"}
   :bootable      true})

(let [vi (assoc valid-vi-entry
           :id "VolumeImage/10"
           :resourceURI vi/type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check VolumeImage vi))
  (expect (s/check VolumeImage (dissoc vi :state)))
  (expect (s/check VolumeImage (dissoc vi :imageLocation)))
  (expect (s/check VolumeImage (assoc vi :imageLocation {})))
  (expect (s/check VolumeImage (dissoc vi :bootable))))

;;
;; VolumeTemplate
;;

(def valid-vt-entry
  {:acl          valid-acl
   :volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage  {:href "VolumeImage/mkplaceid"}})

(let [uri (vt/uuid->uri (utils/create-uuid))
      vt (assoc valid-vt-entry
           :id uri
           :resourceURI vt/type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check VolumeTemplate vt))
  (expect nil? (s/check VolumeTemplate (dissoc vt :volumeImage)))
  (expect (s/check VolumeTemplate (dissoc vt :volumeConfig))))

;;
;; Volume
;;

(def valid-v-entry
  {:acl      valid-acl
   :state    "CREATING"
   :type     "http://schemas.cimi.stratuslab.eu/normal"
   :capacity 1024
   :bootable true
   :eventLog "EventLog/uuid"})

(let [volume (assoc valid-v-entry
               :id "Volume/10"
               :resourceURI v/type-uri
               :created #inst "1964-08-25T10:00:00.0Z"
               :updated #inst "1964-08-25T10:00:00.0Z")]

  (expect nil? (s/check Volume volume))
  (expect nil? (s/check Volume (dissoc volume :state)))
  (expect nil? (s/check Volume (dissoc volume :bootable)))
  (expect nil? (s/check Volume (dissoc volume :eventLog)))
  (expect (s/check Volume (dissoc volume :type)))
  (expect (s/check Volume (dissoc volume :capacity))))

;;
;; Job
;;

(def valid-job-entry
  {:acl                valid-acl
   :state              "QUEUED"
   :targetResource     "Machine/uuid-1"
   :affectedResources  ["Machine/uuid-2"]
   :action             "http://schemas.cimi.stratuslab.eu/create-volume"
   :returnCode         0
   :progress           0
   :statusMessage      "none"
   :timeOfStatusChange #inst "2013-08-25T10:00:00.00Z"
   :parentJob          "Job/uuid-1"
   :nestedJobs         ["Job/uuid-2"]})

(let [job (assoc valid-job-entry
            :id "/Job/10"
            :resourceURI job/type-uri
            :created #inst "1964-08-25T10:00:00.0Z"
            :updated #inst "1964-08-25T10:00:00.0Z")]

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
           :resourceURI mc/type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z"
           :disks [{:capacity 1024
                    :format   "ext4"}])]

  (expect nil? (s/check MachineConfiguration mc))
  (expect nil? (s/check MachineConfiguration (dissoc mc :disks)))
  (expect (s/check MachineConfiguration (dissoc mc :cpu)))
  (expect (s/check MachineConfiguration (dissoc mc :memory)))
  (expect (s/check MachineConfiguration (dissoc mc :cpuArch))))

;;
;; MachineImage
;;

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
           :resourceURI mi/type-uri
           :created #inst "1964-08-25T10:00:00.0Z"
           :updated #inst "1964-08-25T10:00:00.0Z"
           )]

  (expect nil? (s/check MachineImage mi))
  (expect nil? (s/check MachineImage (dissoc mi :imageLocation)))
  (expect nil? (s/check MachineImage (dissoc mi :relatedImage)))
  (expect (s/check MachineImage (dissoc mi :state)))
  (expect (s/check MachineImage (dissoc mi :type)))
  (expect (s/check MachineImage (assoc mi :state "IMAGE"))))

;;
;; Service Message
;;

(let [timestamp #inst "1964-08-25T10:00:00.0Z"
      uri (sm/uuid->uri timestamp)
      sm {:acl         valid-acl
          :id          uri
          :resourceURI sm/type-uri
          :created     timestamp
          :updated     timestamp
          :title       "title"
          :message     "message"}]

  (expect nil? (s/check ServiceMessage sm))
  (expect (s/check ServiceMessage (dissoc sm :title)))
  (expect (s/check ServiceMessage (dissoc sm :message))))


(run-tests [*ns*])

