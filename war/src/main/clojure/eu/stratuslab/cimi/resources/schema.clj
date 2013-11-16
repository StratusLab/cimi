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

(ns eu.stratuslab.cimi.resources.schema
  "Data, definitions, and utilities common to all resources."
  (:require
    [clojure.tools.logging :refer [debug info error]]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all])
  (:import
    clojure.lang.Keyword))

(def ^:const resource-uri "http://schemas.dmtf.org/cimi/1/")

(def ^:const valid-actions
  #{:add :edit :delete
    :start :stop :restart :pause :suspend
    :export :import :capture :snapshot})

(def ^:const action-uri
  (let [root "http://schemas.dmtf.org/cimi/1/Action/"
        m (into {} (map (fn [k] [k (str root (name k))]) valid-actions))]
    (assoc m :add "add" :edit "edit" :delete "delete")))

(def-map-schema
  ^{:doc "Link to another resource."}
  ResourceLink
  [[:href] NonEmptyString])

(def-map-schema Operation
                [[:rel] (set (vals action-uri))
                 [:href] NonEmptyString])

(def-seq-schema Operations
                (constraints (fn [s] (pos? (count s))))
                [Operation])

(def-map-schema Properties
                (constraints (fn [m] (pos? (count (keys m)))))
                [[(wild (:or Keyword String))] String])

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; StratusLab implementation.
;;

(def access-control-types #{"USER" "ROLE"})

(def access-control-rights #{"ALL" "VIEW" "MODIFY"})

(def-map-schema AccessControlId
                [[:principal] NonEmptyString
                 [:type] access-control-types])

(def-map-schema AccessControlRule
                AccessControlId
                [[:right] access-control-rights])

(def-seq-schema AccessControlRules
                (constraints (fn [s] (pos? (count s))))
                AccessControlRule)

(def-map-schema AccessControlList
                [[:owner] AccessControlId
                 (optional-path [:rules]) AccessControlRules])

;;
;; These attributes are common to all resources except the 
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;
(def-map-schema CommonAttrs
                [[:acl] AccessControlList  ;; StratusLab addition
                 [:id] NonEmptyString
                 [:resourceURI] NonEmptyString
                 (optional-path [:name]) NonEmptyString
                 (optional-path [:description]) NonEmptyString
                 [:created] NonEmptyString
                 [:updated] NonEmptyString
                 (optional-path [:properties]) Properties
                 (optional-path [:operations]) Operations])

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(def-map-schema CreateAttrs
                [[:resourceURI] NonEmptyString
                 (optional-path [:name]) NonEmptyString
                 (optional-path [:description]) NonEmptyString
                 (optional-path [:created]) NonEmptyString
                 (optional-path [:updated]) NonEmptyString
                 (optional-path [:properties]) Properties
                 (optional-path [:operations]) Operations])

;;
;; User records within the database.  (StratusLab extension.)
;;
(def-seq-schema Roles
                (constraints (fn [s] (pos? (count s))))
                [NonEmptyString])

(def-map-schema Altnames
                (constraints (fn [m] (pos? (count (keys m)))))
                [[(wild Keyword)] NonEmptyString])

(def-map-schema User :loose
                CommonAttrs
                [[:first-name] NonEmptyString
                 [:last-name] NonEmptyString
                 [:username] NonEmptyString
                 (optional-path [:password]) NonEmptyString
                 (optional-path [:enabled]) Boolean
                 (optional-path [:roles]) Roles
                 (optional-path [:altnames]) Altnames])

;;
;; Service configuration files.  (StratusLab extension.)
;;
(def-map-schema ServiceConfiguration :loose
                CommonAttrs
                [[:service] NonEmptyString
                 (optional-path [:instance]) NonEmptyString])

;;
;; Cloud Entry Point Schema
;;

(def-map-schema CloudEntryPoint
                CommonAttrs
                [[:baseURI] NonEmptyString
                 (optional-path [:resourceMetadata]) ResourceLink
                 (optional-path [:systems]) ResourceLink
                 (optional-path [:systemTemplates]) ResourceLink
                 (optional-path [:machines]) ResourceLink
                 (optional-path [:machineTemplates]) ResourceLink
                 (optional-path [:machineConfigs]) ResourceLink
                 (optional-path [:machineImages]) ResourceLink
                 (optional-path [:credentials]) ResourceLink
                 (optional-path [:credentialTemplates]) ResourceLink
                 (optional-path [:volumes]) ResourceLink
                 (optional-path [:volumeTemplates]) ResourceLink
                 (optional-path [:volumeConfigs]) ResourceLink
                 (optional-path [:volumeImages]) ResourceLink
                 (optional-path [:networks]) ResourceLink
                 (optional-path [:networkTemplates]) ResourceLink
                 (optional-path [:networkConfigs]) ResourceLink
                 (optional-path [:networkPorts]) ResourceLink
                 (optional-path [:networkPortTemplates]) ResourceLink
                 (optional-path [:networkPortConfigs]) ResourceLink
                 (optional-path [:addresses]) ResourceLink
                 (optional-path [:addressTemplates]) ResourceLink
                 (optional-path [:forwardingGroups]) ResourceLink
                 (optional-path [:forwardingGroupTemplates]) ResourceLink
                 (optional-path [:jobs]) ResourceLink
                 (optional-path [:meters]) ResourceLink
                 (optional-path [:meterTemplates]) ResourceLink
                 (optional-path [:meterConfigs]) ResourceLink
                 (optional-path [:eventLogs]) ResourceLink
                 (optional-path [:eventLogTemplates]) ResourceLink
                 (optional-path [:serviceConfigurations]) ResourceLink
                 (optional-path [:serviceMessages]) ResourceLink
                 (optional-path [:users]) ResourceLink])

;;
;; Volume Related Schemas
;;

(def image-states #{"CREATING" "AVAILABLE" "DELETING" "ERROR"})

(def volume-states #{"CREATING" "AVAILABLE" "CAPTURING" "DELETING" "ERROR"})

(def-map-schema VolumeConfigurationAttrs
                [(optional-path [:type]) NonEmptyString
                 (optional-path [:format]) NonEmptyString
                 (optional-path [:capacity]) NonNegIntegral])

(def-map-schema VolumeConfiguration
                CommonAttrs
                [(optional-path [:type]) NonEmptyString
                 (optional-path [:format]) NonEmptyString
                 [:capacity] NonNegIntegral])

(def-map-schema VolumeImageAttrs
                [(optional-path [:state]) image-states
                 (optional-path [:imageLocation]) ResourceLink
                 (optional-path [:bootable]) Boolean])

(def-map-schema VolumeImage
                CommonAttrs
                [[:state] image-states
                 [:imageLocation] ResourceLink
                 [:bootable] Boolean])

(def-map-schema VolumeConfigurationRef
                VolumeConfigurationAttrs
                [(optional-path [:href]) NonEmptyString])

(def-map-schema VolumeImageRef
                VolumeImageAttrs
                [(optional-path [:href]) NonEmptyString])

;; TODO: Add real schema once Meters are supported.
(def MeterTemplateRef
  Anything)

(def-seq-schema MeterTemplateRefs
                (constraints (fn [refs] (pos? (count refs))))
                MeterTemplateRef)

;; TODO: Add real schema once EventLogs are supported.
(def EventLogTemplateRef
  Anything)

(def-map-schema VolumeTemplateAttrs
                [(optional-path [:volumeConfig]) VolumeConfigurationRef
                 (optional-path [:volumeImage]) VolumeImageRef
                 (optional-path [:meterTemplates]) MeterTemplateRefs
                 (optional-path [:eventLogTemplate]) EventLogTemplateRef])

(def-map-schema VolumeTemplate
                CommonAttrs
                [[:volumeConfig] VolumeConfigurationRef
                 (optional-path [:volumeImage]) VolumeImageRef
                 (optional-path [:meterTemplates]) MeterTemplateRefs
                 (optional-path [:eventLogTemplate]) EventLogTemplateRef])

(def-map-schema ^{:doc "Documentation"} Volume
                CommonAttrs
                [(optional-path [:state]) volume-states
                 [:type] NonEmptyString
                 [:capacity] NonNegIntegral
                 (optional-path [:bootable]) Boolean
                 (optional-path [:eventLog]) NonEmptyString])

(def-map-schema VolumeTemplateRef
                VolumeTemplateAttrs
                [(optional-path [:href]) NonEmptyString])

(def-map-schema VolumeCreate
                CreateAttrs
                [[:volumeTemplate] VolumeTemplateRef])

;;
;; Jobs
;;

(def job-states #{"QUEUED" "RUNNING" "FAILED" "SUCCESS" "STOPPING" "STOPPED"})

(def-map-schema Job
                CommonAttrs
                [(optional-path [:state]) job-states
                 [:targetResource] NonEmptyString
                 (optional-path [:affectedResources]) (sequence-of NonEmptyString)
                 [:action] NonEmptyString
                 (optional-path [:returnCode]) Integral
                 (optional-path [:progress]) NonNegIntegral
                 (optional-path [:statusMessage]) NonEmptyString
                 (optional-path [:timeOfStatusChange]) NonEmptyString
                 (optional-path [:parentJob]) NonEmptyString
                 (optional-path [:nestedJobs]) (sequence-of NonEmptyString)])

;;
;; Event
;;

(def-map-schema StateContent
                [[:resName] NonEmptyString
                 [:resource] NonEmptyString
                 [:resType] NonEmptyString
                 [:state] NonEmptyString
                 (optional-path [:previous]) NonEmptyString])

(def-map-schema AlarmContent
                [[:resName] NonEmptyString
                 [:resource] NonEmptyString
                 [:resType] NonEmptyString
                 [:code] NonEmptyString
                 (optional-path [:detail]) NonEmptyString])

(def-map-schema ModelContent
                [[:resName] NonEmptyString
                 [:resource] NonEmptyString
                 [:resType] NonEmptyString
                 [:change] NonEmptyString
                 (optional-path [:detail]) NonEmptyString])

(def-map-schema AccessContent
                [[:operation] NonEmptyString
                 [:resource] NonEmptyString
                 (optional-path [:detail]) NonEmptyString
                 [:initiator] NonEmptyString])

(def-map-schema Event
                CommonAttrs
                [[:timestamp] NonEmptyString
                 [:type] NonEmptyString
                 (optional-path [:content]) (or StateContent AlarmContent ModelContent AccessContent)
                 [:outcome] #{"Pending" "Unknown" "Status" "Success" "Warning" "Failure"}
                 [:severity] #{"critical" "high" "medium" "low"}
                 (optional-path [:contact]) String])

;;
;; MachineConfiguration
;;

(def-map-schema Disk
                [[:capacity] PosIntegral
                 [:format] NonEmptyString
                 (optional-path [:initialLocation]) NonEmptyString])

(def-seq-schema Disks
                (constraints (fn [s] (pos? (count s))))
                [Disk])

(def-map-schema MachineConfiguration
                CommonAttrs
                [[:cpu] PosIntegral
                 [:memory] PosIntegral
                 [:cpuArch] #{"68000" "Alpha" "ARM" "Itanium" "MIPS" "PA_RISC"
                              "POWER" "PowerPC" "x86" "x86_64" "zArchitecture", "SPARC"}
                 (optional-path [:disks]) Disks])

;;
;; MachineImage
;;

(def-map-schema Disk
                [[:capacity] PosIntegral
                 [:format] NonEmptyString
                 (optional-path [:initialLocation]) NonEmptyString])

(def-seq-schema Disks
                (constraints (fn [s] (pos? (count s))))
                [Disk])

(def-map-schema MachineImage
                (constraints (fn [m] (or (not= "IMAGE" (:type m) (nil? (:relatedImage m))))))
                CommonAttrs
                [[:state] #{"CREATING" "AVAILABLE" "DELETING" "ERROR"}
                 [:type] #{"IMAGE" "SNAPSHOT" "PARTIAL_SNAPSHOT"}
                 (optional-path [:imageLocation]) NonEmptyString
                 (optional-path [:relatedImage]) ResourceLink])

;;
;; ServiceMessage
;;

(def-map-schema ServiceMessage
                CommonAttrs
                [[:name] NonEmptyString
                 [:description] NonEmptyString])
