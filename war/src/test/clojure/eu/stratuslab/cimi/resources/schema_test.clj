;
; Copyright 2013 Centre National de la Recherche Scientifique (CNRS)
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

(ns eu.stratuslab.cimi.resources.schema-test
  (:require
    [eu.stratuslab.cimi.resources.schema :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.resources.user :as user]
    [eu.stratuslab.cimi.resources.volume-configuration :as vc]
    [eu.stratuslab.cimi.resources.volume-template :as vt]
    [eu.stratuslab.cimi.resources.volume-image :as vi]
    [eu.stratuslab.cimi.resources.volume :as v]
    [eu.stratuslab.cimi.resources.service-message :as sm]
    [eu.stratuslab.cimi.resources.machine-configuration :as mc]
    [eu.stratuslab.cimi.resources.job :as job]
    [clj-schema.validation :refer [validation-errors]]
    [clojure.test :refer :all]))


(def valid-acl {:owner {:principal "me" :type "USER"}})

(deftest test-access-control-id
  (let [rule {:principal "::ADMIN"
              :type      "ROLE"}]
    (are [v pred] (pred (validation-errors AccessControlId v))
                  rule empty?
                  (assoc rule :bad "MODIFY") (complement empty?)
                  (dissoc rule :principal) (complement empty?)
                  (dissoc rule :type) (complement empty?)
                  (assoc rule :type "USER") empty?
                  (assoc rule :type "BAD") (complement empty?))))

(deftest test-access-control-rule
  (let [rule {:principal "::ADMIN"
              :type      "ROLE"
              :right     "VIEW"}]
    (are [v pred] (pred (validation-errors AccessControlRule v))
                  rule empty?
                  (assoc rule :right "MODIFY") empty?
                  (assoc rule :right "ALL") empty?
                  (assoc rule :right "BAD") (complement empty?)
                  (dissoc rule :right) (complement empty?))))

(deftest test-access-control-rules
  (let [rules [{:principal "::ADMIN"
                :type      "ROLE"
                :right     "VIEW"}

               {:principal "ALPHA"
                :type      "USER"
                :right     "ALL"}]]
    (are [v pred] (pred (validation-errors AccessControlRules v))
                  rules empty?
                  (next rules) empty?
                  (next (next rules)) (complement empty?))))

(deftest test-access-control-list
  (let [acl {:owner {:principal "::ADMIN"
                     :type      "ROLE"}
             :rules [{:principal ":group1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "group2"
                      :type      "ROLE"
                      :right     "MODIFY"}]}]
    (are [v pred] (pred (validation-errors AccessControlList v))
                  acl empty?
                  (dissoc acl :rules) empty?
                  (assoc acl :rules []) (complement empty?)
                  (assoc acl :owner "") (complement empty?)
                  (assoc acl :bad "value") (complement empty?))))

(deftest test-resource-link-schema
  (let [ref {:href "https://example.org/resource"}]
    (are [v pred] (pred (validation-errors ResourceLink v))
                  ref empty?
                  (dissoc ref :href) (complement empty?)
                  (assoc ref :bad "BAD") (complement empty?))))

(deftest test-operation-schema
  (are [v pred] (pred (validation-errors Operation v))
                {:rel "add" :href "/add"} empty?
                {:rel "add"} (complement empty?)
                {:href "/add"} (complement empty?)
                {} (complement empty?)))

(deftest test-operations-schema
  (let [ops [{:rel "add" :href "/add"}
             {:rel "delete" :href "/delete"}]]
    (are [v pred] (pred (validation-errors Operations v))
                  ops empty?
                  (rest ops) empty?
                  [] (complement empty?))))

(deftest test-properties-schema
  (are [v pred] (pred (validation-errors Properties v))
                {"a" "b"} empty?
                {"a" "b", "c" "d"} empty?
                {"a" 1} (complement empty?)
                {} (complement empty?)))

(deftest test-common-attrs-schema
  (let [date "1964-08-25T10:00:00.0Z"
        minimal {:acl         valid-acl
                 :id          "a"
                 :resourceURI "http://example.org/data"
                 :created     date
                 :updated     date}
        maximal (assoc minimal
                  :name "name"
                  :description "description"
                  :properties {"a" "b"}
                  :operations [{:rel "add" :href "/add"}])]

    (are [v pred] (pred (validation-errors CommonAttrs v))
                  minimal empty?
                  (dissoc minimal :id) (complement empty?)
                  (dissoc minimal :resourceURI) (complement empty?)
                  (dissoc minimal :created) (complement empty?)
                  (dissoc minimal :updated) (complement empty?)
                  maximal empty?
                  (dissoc maximal :name) empty?
                  (dissoc maximal :description) empty?
                  (dissoc maximal :properties) empty?
                  (dissoc maximal :operations) empty?
                  (assoc maximal :bad "bad") (complement empty?))))

(deftest test-action-uri-map
  (is (= valid-actions (set (keys action-uri)))))

;;
;; User
;;
(def valid-user-entry
  {:acl        valid-acl
   :first-name "cloud"
   :last-name  "user"
   :username   "cloud-user"})

(deftest test-user-schema
  (let [uri (user/uuid->uri "cloud-user")
        user (assoc valid-user-entry
               :id uri
               :resourceURI user/type-uri
               :created "1964-08-25T10:00:00.0Z"
               :updated "1964-08-25T10:00:00.0Z")]
    (are [v pred] (pred (validation-errors User v))
                  user empty?
                  (assoc user :password "password") empty?
                  (assoc user :active true) empty?
                  (assoc user :active "BAD") (complement empty?)
                  (assoc user :roles ["OK"]) empty?
                  (assoc user :roles []) (complement empty?)
                  (assoc user :roles "BAD") (complement empty?)
                  (assoc user :altnames {:x500dn "certdn"}) empty?
                  (assoc user :altnames {}) (complement empty?)
                  (assoc user :email "OK@EXAMPLE.COM") empty?
                  (dissoc user :acl) (complement empty?)
                  (dissoc user :first-name) (complement empty?)
                  (dissoc user :last-name) (complement empty?)
                  (dissoc user :username) (complement empty?))))

;;
;; VolumeConfiguration
;;

(def valid-vc-entry
  {:acl      valid-acl
   :type     "http://stratuslab.eu/cimi/1/raw"
   :format   "ext4"
   :capacity 1000})

(deftest test-volume-configuration-schema
  (let [uri (vc/uuid->uri (utils/create-uuid))
        volume-configuration (assoc valid-vc-entry
                               :id uri
                               :resourceURI vc/type-uri
                               :created "1964-08-25T10:00:00.0Z"
                               :updated "1964-08-25T10:00:00.0Z")]
    (are [v pred] (pred (validation-errors VolumeConfiguration v))
                  volume-configuration empty?
                  (dissoc volume-configuration :type) empty?
                  (dissoc volume-configuration :format) empty?
                  (dissoc volume-configuration :capacity) (complement empty?))))

;;
;; VolumeImage
;;

(def valid-vi-entry
  {:acl           valid-acl
   :state         "CREATING"
   :imageLocation {:href "GWE_nifKGCcXiFk42XaLrS8LQ-J"}
   :bootable      true})

(deftest test-volume-image-schema
  (let [volume-image (assoc valid-vi-entry
                       :id "VolumeImage/10"
                       :resourceURI vi/type-uri
                       :created "1964-08-25T10:00:00.0Z"
                       :updated "1964-08-25T10:00:00.0Z")]

    (are [v pred] (pred (validation-errors VolumeImage v))
                  volume-image empty?
                  (dissoc volume-image :state) (complement empty?)
                  (dissoc volume-image :imageLocation) (complement empty?)
                  (assoc volume-image :imageLocation {}) (complement empty?)
                  (dissoc volume-image :bootable) (complement empty?))))

;;
;; VolumeTemplate
;;

(def valid-vt-entry
  {:acl          valid-acl
   :volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage  {:href "VolumeImage/mkplaceid"}})

(deftest test-volume-template-schema
  (let [uri (vt/uuid->uri (utils/create-uuid))
        volume-template (assoc valid-vt-entry
                          :id uri
                          :resourceURI vt/type-uri
                          :created "1964-08-25T10:00:00.0Z"
                          :updated "1964-08-25T10:00:00.0Z")]

    (are [v pred] (pred (validation-errors VolumeTemplate v))
                  volume-template empty?
                  (dissoc volume-template :volumeConfig) (complement empty?)
                  (dissoc volume-template :volumeImage) empty?)))

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

(deftest test-volume-schema
  (let [volume (assoc valid-v-entry
                 :id "Volume/10"
                 :resourceURI v/type-uri
                 :created "1964-08-25T10:00:00.0Z"
                 :updated "1964-08-25T10:00:00.0Z")]

    (are [v pred] (pred (validation-errors Volume v))
                  volume empty?
                  (dissoc volume :state) empty?
                  (dissoc volume :bootable) empty?
                  (dissoc volume :eventLog) empty?
                  (dissoc volume :type) (complement empty?)
                  (dissoc volume :capacity) (complement empty?))))

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
   :timeOfStatusChange "20130825T10:00:00.00Z"
   :parentJob          "Job/uuid-1"
   :nestedJobs         ["Job/uuid-2"]})

(deftest test-job-schema
  (let [job (assoc valid-job-entry
              :id "/Job/10"
              :resourceURI job/type-uri
              :created "1964-08-25T10:00:00.0Z"
              :updated "1964-08-25T10:00:00.0Z")]

    (are [v pred] (pred (validation-errors Job v))
                  job empty?
                  (dissoc job :state) empty?
                  (dissoc job :affectedResources) empty?
                  (dissoc job :returnCode) empty?
                  (dissoc job :progress) empty?
                  (dissoc job :statusMessage) empty?
                  (dissoc job :timeOfStatusChange) empty?
                  (dissoc job :parentJob) empty?
                  (dissoc job :nestedJobs) empty?
                  (dissoc job :targetResource) (complement empty?)
                  (dissoc job :action) (complement empty?))))

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

(deftest test-disk-schema
  (let [disk {:capacity 1024 :format "ext4" :initialLocation "/dev/hda"}]

    (are [v pred] (pred (validation-errors Disk v))
                  disk empty?
                  (dissoc disk :initialLocation) empty?
                  (dissoc disk :capacity) (complement empty?)
                  (dissoc disk :format) (complement empty?)
                  {} (complement empty?))))

(deftest test-disks-schema
  (let [disks [{:capacity 1024 :format "ext4" :initialLocation "/dev/hda"}
               {:capacity 2048 :format "swap" :initialLocation "/dev/hdb"}]]

    (are [v pred] (pred (validation-errors Disks v))
                  disks empty?
                  (rest disks) empty?
                  [] (complement empty?))))

(deftest test-machine-configuration-schema
  (let [mc (assoc valid-mc-entry
             :id "/MachineConfiguration/10"
             :resourceURI mc/type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z"
             :disks [{:capacity 1024
                      :format   "ext4"}])]

    (are [v pred] (pred (validation-errors MachineConfiguration v))
                  mc empty?
                  (dissoc mc :disks) empty?
                  (dissoc mc :cpu) (complement empty?)
                  (dissoc mc :memory) (complement empty?)
                  (dissoc mc :cpuArch) (complement empty?)
                  (dissoc mc :cpu) (complement empty?))))

;;
;; Service Message
;;

(deftest test-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        uri (sm/uuid->uri timestamp)
        service-message {:acl         valid-acl
                         :id          uri
                         :resourceURI sm/type-uri
                         :created     timestamp
                         :updated     timestamp
                         :name        "title"
                         :description "description"}]

    (are [v pred] (pred (validation-errors ServiceMessage v))
                  service-message empty?
                  (dissoc service-message :name) (complement empty?)
                  (dissoc service-message :description) (complement empty?))))

