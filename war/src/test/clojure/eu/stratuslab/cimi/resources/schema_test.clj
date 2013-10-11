(ns eu.stratuslab.cimi.resources.schema-test
  (:require
    [eu.stratuslab.cimi.resources.schema :refer :all]
    [eu.stratuslab.cimi.resources.utils :as utils]
    [eu.stratuslab.cimi.resources.volume-configuration :as vc]
    [eu.stratuslab.cimi.resources.volume-template :as vt]
    [eu.stratuslab.cimi.resources.volume-image :as vi]
    [eu.stratuslab.cimi.resources.volume :as v]
    [eu.stratuslab.cimi.resources.service-message :as sm]
    [eu.stratuslab.cimi.resources.machine-configuration :as mc]
    [eu.stratuslab.cimi.resources.job :as job]
    [clj-schema.validation :refer [validation-errors]]
    [clojure.test :refer :all]))

(deftest test-resource-link-schema
  (let [ref {:href "https://example.org/resource"}]
    (is (empty? (validation-errors ResourceLink ref)))
    (is (not (empty? (validation-errors ResourceLink (dissoc ref :href)))))
    (is (not (empty? (validation-errors ResourceLink (assoc ref :bad "BAD")))))))

(deftest test-operation-schema
  (is (empty? (validation-errors Operation {:rel "add" :href "/add"})))
  (is (not (empty? (validation-errors Operation {:rel "add"}))))
  (is (not (empty? (validation-errors Operation {:href "/add"}))))
  (is (not (empty? (validation-errors Operation {}))))
  )

(deftest test-operations-schema
  (let [ops [{:rel "add" :href "/add"}
             {:rel "delete" :href "/delete"}]]
    (is (empty? (validation-errors Operations ops)))
    (is (empty? (validation-errors Operations (rest ops))))
    (is (not (empty? (validation-errors Operations []))))))

(deftest test-properties-schema
  (is (empty? (validation-errors Properties {"a" "b"})))
  (is (empty? (validation-errors Properties {"a" "b", "c" "d"})))
  (is (not (empty? (validation-errors Properties {})))))

(deftest test-common-attrs-schema
  (let [date "1964-08-25T10:00:00.0Z"
        minimal {:id "a"
                 :resourceURI "http://example.org/data"
                 :created date
                 :updated date}
        maximal (assoc minimal
                  :name "name"
                  :description "description"
                  :properties {"a" "b"}
                  :operations [{:rel "add" :href "/add"}])]
    (is (empty? (validation-errors CommonAttrs minimal)))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :id)))))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :resourceURI)))))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :created)))))
    (is (not (empty? (validation-errors CommonAttrs (dissoc minimal :updated)))))

    (is (empty? (validation-errors CommonAttrs maximal)))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :name))))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :description))))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :properties))))
    (is (empty? (validation-errors CommonAttrs (dissoc maximal :operations))))
    (is (not (empty? (validation-errors CommonAttrs (assoc maximal :bad "bad")))))
    )
  )

(deftest test-action-uri-map
  (is (= valid-actions (set (keys action-uri)))))

;;
;; VolumeConfiguration
;;

(def valid-vc-entry
  {:type "http://stratuslab.eu/cimi/1/raw"
   :format "ext4"
   :capacity 1000})

(deftest test-volume-configuration-schema
  (let [uri (vc/uuid->uri (utils/create-uuid))
        volume-configuration (assoc valid-vc-entry
                               :id uri
                               :resourceURI vc/type-uri
                               :created "1964-08-25T10:00:00.0Z"
                               :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors VolumeConfiguration volume-configuration)))
    (is (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :type))))
    (is (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :format))))
    (is (not (empty? (validation-errors VolumeConfiguration (dissoc volume-configuration :capacity)))))))

;;
;; VolumeImage
;;

(def valid-vi-entry
  {:state "CREATING"
   :imageLocation {:href "GWE_nifKGCcXiFk42XaLrS8LQ-J"}
   :bootable true})

(deftest test-volume-image-schema
  (let [volume-image (assoc valid-vi-entry
                       :id "VolumeImage/10"
                       :resourceURI vi/type-uri
                       :created "1964-08-25T10:00:00.0Z"
                       :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors VolumeImage volume-image)))
    (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :state)))))
    (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :imageLocation)))))
    (is (not (empty? (validation-errors VolumeImage (assoc volume-image :imageLocation {})))))
    (is (not (empty? (validation-errors VolumeImage (dissoc volume-image :bootable)))))))

;;
;; VolumeTemplate
;;

(def valid-vt-entry
  {:volumeConfig {:href "VolumeConfiguration/uuid"}
   :volumeImage {:href "VolumeImage/mkplaceid"}})

(deftest test-volume-template-schema
  (let [uri (vt/uuid->uri (utils/create-uuid))
        volume-template (assoc valid-vt-entry
                          :id uri
                          :resourceURI vt/type-uri
                          :created "1964-08-25T10:00:00.0Z"
                          :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors VolumeTemplate volume-template)))
    (is (not (empty? (validation-errors VolumeTemplate (dissoc volume-template :volumeConfig)))))
    (is (empty? (validation-errors VolumeTemplate (dissoc volume-template :volumeImage))))))

;;
;; Volume
;;

(def valid-v-entry
  {:state "CREATING"
   :type "http://schemas.cimi.stratuslab.eu/normal"
   :capacity 1024
   :bootable true
   :eventLog "EventLog/uuid"})

(deftest test-volume-schema
  (let [volume (assoc valid-v-entry
                 :id "Volume/10"
                 :resourceURI v/type-uri
                 :created "1964-08-25T10:00:00.0Z"
                 :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors Volume volume)))
    (is (empty? (validation-errors Volume (dissoc volume :state))))
    (is (empty? (validation-errors Volume (dissoc volume :bootable))))
    (is (empty? (validation-errors Volume (dissoc volume :eventLog))))
    (is (not (empty? (validation-errors Volume (dissoc volume :type)))))
    (is (not (empty? (validation-errors Volume (dissoc volume :capacity)))))))

;;
;; Job
;;

(def valid-job-entry
  {:state "QUEUED"
   :targetResource "Machine/uuid-1"
   :affectedResources ["Machine/uuid-2"]
   :action "http://schemas.cimi.stratuslab.eu/create-volume"
   :returnCode 0
   :progress 0
   :statusMessage "none"
   :timeOfStatusChange "20130825T10:00:00.00Z"
   :parentJob "Job/uuid-1"
   :nestedJobs ["Job/uuid-2"]})

(deftest test-job-schema
  (let [job (assoc valid-job-entry
              :id "/Job/10"
              :resourceURI job/type-uri
              :created "1964-08-25T10:00:00.0Z"
              :updated "1964-08-25T10:00:00.0Z")]
    (is (empty? (validation-errors Job job)))
    (is (empty? (validation-errors Job (dissoc job :state))))
    (is (empty? (validation-errors Job (dissoc job :affectedResources))))
    (is (empty? (validation-errors Job (dissoc job :returnCode))))
    (is (empty? (validation-errors Job (dissoc job :progress))))
    (is (empty? (validation-errors Job (dissoc job :statusMessage))))
    (is (empty? (validation-errors Job (dissoc job :timeOfStatusChange))))
    (is (empty? (validation-errors Job (dissoc job :parentJob))))
    (is (empty? (validation-errors Job (dissoc job :nestedJobs))))
    (is (not (empty? (validation-errors Job (dissoc job :targetResource)))))
    (is (not (empty? (validation-errors Job (dissoc job :action)))))))

;;
;; MachineConfiguration
;;

(def valid-mc-entry
  {:name "valid"
   :description "valid machine configuration"
   :cpu 1
   :memory 512000
   :cpuArch "x86_64"
   :disks [{:capacity 1024
            :format "ext4"
            :initialLocation "/dev/hda"}]})

(deftest test-disk-schema
  (let [disk {:capacity 1024 :format "ext4" :initialLocation "/dev/hda"}]
    (is (empty? (validation-errors Disk disk)))
    (is (empty? (validation-errors Disk (dissoc disk :initialLocation))))
    (is (not (empty? (validation-errors Disk (dissoc disk :capacity)))))
    (is (not (empty? (validation-errors Disk (dissoc disk :format)))))
    (is (not (empty? (validation-errors Disk {})))))
  )

(deftest test-disks-schema
  (let [disks [{:capacity 1024 :format "ext4" :initialLocation "/dev/hda"}
               {:capacity 2048 :format "swap" :initialLocation "/dev/hdb"}]]
    (is (empty? (validation-errors Disks disks)))
    (is (empty? (validation-errors Disks (rest disks))))
    (is (not (empty? (validation-errors Disks []))))))

(deftest test-machine-configuration-schema
  (let [mc (assoc valid-mc-entry
             :id "/MachineConfiguration/10"
             :resourceURI mc/type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z"
             :disks [{:capacity 1024
                      :format "ext4"}])]
    (is (empty? (validation-errors MachineConfiguration mc)))
    (is (empty? (validation-errors MachineConfiguration (dissoc mc :disks))))
    (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :cpu)))))
    (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :memory)))))
    (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :cpuArch)))))
    (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :cpu)))))))

;;
;; Service Message
;;

(def valid-sm-entry
  {:name "title"
   :description "description"})

(deftest test-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        uri (sm/uuid->uri timestamp)
        service-message {:id uri
                         :resourceURI sm/type-uri
                         :created timestamp
                         :updated timestamp
                         :name "title"
                         :description "description"}]
    (is (empty? (validation-errors ServiceMessage service-message)))
    (is (not (empty? (validation-errors ServiceMessage (dissoc service-message :name)))))
    (is (not (empty? (validation-errors ServiceMessage (dissoc service-message :description)))))))

