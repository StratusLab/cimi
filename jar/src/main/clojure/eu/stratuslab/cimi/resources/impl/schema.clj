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

(ns eu.stratuslab.cimi.resources.impl.schema
  "Data, definitions, and utilities common to all resources."
  (:require
    [clojure.tools.logging :refer [debug info error]]
    [clojure.string :as str]
    [schema.core :as s]))

(def ^:const resource-uri "http://schemas.dmtf.org/cimi/1/")

(def ^:const valid-actions
  #{:add :edit :delete
    :start :stop :restart :pause :suspend
    :export :import :capture :snapshot})

(def ^:const action-uri
  (let [root "http://schemas.dmtf.org/cimi/1/Action/"
        m (into {} (map (fn [k] [k (str root (name k))]) valid-actions))]
    (assoc m :add "add" :edit "edit" :delete "delete")))

(def ^:const valid-action-uris
  (vals action-uri))

(def NotEmpty
  (s/pred seq "not-empty?"))

(def PosInt
  (s/both s/Int (s/pred pos? "pos?")))

(def NonNegInt
  (s/both s/Int (s/pred (complement neg?) "not-neg?")))

(def NonBlankString
  (s/both s/Str (s/pred (complement str/blank?) "not-blank?")))

(def NonEmptyStrList
  (s/both [NonBlankString] NotEmpty))

(def ValidCpuArch
  (s/enum "68000" "Alpha" "ARM" "Itanium" "MIPS" "PA_RISC"
          "POWER" "PowerPC" "x86" "x86_64" "zArchitecture", "SPARC"))

(def ResourceLink
  {:href NonBlankString})

(def ResourceLinks
  (s/both [ResourceLink] NotEmpty))

(def Operation
  (merge ResourceLink {:rel (apply s/enum valid-action-uris)}))

(def Operations
  (s/both [Operation] NotEmpty))

(def Properties
  (s/both
    {(s/either s/Keyword s/Str) s/Str}
    NotEmpty))

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; StratusLab implementation.
;;

(def access-control-types (s/enum "USER" "ROLE"))

(def access-control-rights (s/enum "ALL" "VIEW" "MODIFY"))

(def AccessControlId
  {:principal NonBlankString
   :type      access-control-types})

(def AccessControlRule
  (merge AccessControlId {:right access-control-rights}))

(def AccessControlRules
  (s/both [AccessControlRule] NotEmpty))

(def AccessControlList
  {:owner                  AccessControlId
   (s/optional-key :rules) AccessControlRules})

;;
;; These attributes are common to all resources except the
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;
(def CommonAttrs
  {:acl                          AccessControlList          ;; StratusLab addition
   :id                           NonBlankString
   :resourceURI                  NonBlankString
   (s/optional-key :name)        NonBlankString
   (s/optional-key :description) NonBlankString
   :created                      s/Inst
   :updated                      s/Inst
   (s/optional-key :properties)  Properties
   (s/optional-key :operations)  Operations})

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(def CreateAttrs
  {:resourceURI                  NonBlankString
   (s/optional-key :name)        NonBlankString
   (s/optional-key :description) NonBlankString
   (s/optional-key :created)     s/Inst
   (s/optional-key :updated)     s/Inst
   (s/optional-key :properties)  Properties
   (s/optional-key :operations)  Operations})

;;
;; User records within the database.  (StratusLab extension.)
;;
(def Roles
  (s/both
    [NonBlankString]
    NotEmpty))

(def Altnames
  (s/both
    {s/Keyword NonBlankString}
    NotEmpty))

(def User
  (merge CommonAttrs
         {:first-name                NonBlankString
          :last-name                 NonBlankString
          :username                  NonBlankString
          (s/optional-key :password) NonBlankString
          (s/optional-key :enabled)  s/Bool
          (s/optional-key :roles)    Roles
          (s/optional-key :altnames) Altnames
          (s/optional-key :email)    NonBlankString}))

;;
;; Service configuration files.  (StratusLab extension.)
;;
(def ServiceConfiguration
  (merge CommonAttrs
         {:service                   NonBlankString
          (s/optional-key :instance) NonBlankString}))

;;
;; Cloud Entry Point Schema
;;

(def CloudEntryPoint
  (merge CommonAttrs
         {:baseURI                                   NonBlankString
          (s/optional-key :resourceMetadata)         ResourceLink
          (s/optional-key :systems)                  ResourceLink
          (s/optional-key :systemTemplates)          ResourceLink
          (s/optional-key :machines)                 ResourceLink
          (s/optional-key :machineTemplates)         ResourceLink
          (s/optional-key :machineConfigs)           ResourceLink
          (s/optional-key :machineImages)            ResourceLink
          (s/optional-key :credentials)              ResourceLink
          (s/optional-key :credentialTemplates)      ResourceLink
          (s/optional-key :volumes)                  ResourceLink
          (s/optional-key :volumeTemplates)          ResourceLink
          (s/optional-key :volumeConfigs)            ResourceLink
          (s/optional-key :volumeImages)             ResourceLink
          (s/optional-key :networks)                 ResourceLink
          (s/optional-key :networkTemplates)         ResourceLink
          (s/optional-key :networkConfigs)           ResourceLink
          (s/optional-key :networkPorts)             ResourceLink
          (s/optional-key :networkPortTemplates)     ResourceLink
          (s/optional-key :networkPortConfigs)       ResourceLink
          (s/optional-key :addresses)                ResourceLink
          (s/optional-key :addressTemplates)         ResourceLink
          (s/optional-key :forwardingGroups)         ResourceLink
          (s/optional-key :forwardingGroupTemplates) ResourceLink
          (s/optional-key :jobs)                     ResourceLink
          (s/optional-key :meters)                   ResourceLink
          (s/optional-key :meterTemplates)           ResourceLink
          (s/optional-key :meterConfigs)             ResourceLink
          (s/optional-key :eventLogs)                ResourceLink
          (s/optional-key :eventLogTemplates)        ResourceLink
          (s/optional-key :serviceConfigurations)    ResourceLink ;; StratusLab extension
          (s/optional-key :serviceMessages)          ResourceLink ;; StratusLab extension
          (s/optional-key :users)                    ResourceLink ;; StratusLab extension
          }))

;;
;; Volume Related Schemas
;;

(def image-states (s/enum "CREATING" "AVAILABLE" "DELETING" "ERROR"))

(def volume-states (s/enum "CREATING" "AVAILABLE" "CAPTURING" "DELETING" "ERROR"))

(def VolumeConfigurationAttrs
  {(s/optional-key :type)     NonBlankString
   (s/optional-key :format)   NonBlankString
   (s/optional-key :capacity) PosInt})

(def VolumeConfigurationAttrs
  {(s/optional-key :type)     NonBlankString
   (s/optional-key :format)   NonBlankString
   (s/optional-key :capacity) PosInt})

(def VolumeConfiguration
  (merge CommonAttrs
         {(s/optional-key :type)   NonBlankString
          (s/optional-key :format) NonBlankString
          :capacity                PosInt}))

(def VolumeImageAttrs
  {(s/optional-key :state)         image-states
   (s/optional-key :imageLocation) ResourceLink
   (s/optional-key :bootable)      s/Bool})

(def VolumeImage
  (merge CommonAttrs
         {:state         image-states
          :imageLocation ResourceLink
          :bootable      s/Bool}))

(def VolumeConfigurationRef
  (merge VolumeConfigurationAttrs
         {(s/optional-key :href) NonBlankString}))

(def VolumeImageRef
  (merge VolumeImageAttrs
         {(s/optional-key :href) NonBlankString}))

;; TODO: Add real schema once Meters are supported.
(def MeterTemplateRef
  {})

(def MeterTemplateRefs
  (s/both [MeterTemplateRef] NotEmpty))

;; TODO: Add real schema once EventLogs are supported.
(def EventLogTemplateRef
  {})

(def VolumeTemplateAttrs
  {(s/optional-key :volumeConfig)     VolumeConfigurationRef
   (s/optional-key :volumeImage)      VolumeImageRef
   (s/optional-key :meterTemplates)   MeterTemplateRefs
   (s/optional-key :eventLogTemplate) EventLogTemplateRef})

(def VolumeTemplate
  (merge CommonAttrs
         {:volumeConfig                      VolumeConfigurationRef
          (s/optional-key :volumeImage)      VolumeImageRef
          (s/optional-key :meterTemplates)   MeterTemplateRefs
          (s/optional-key :eventLogTemplate) EventLogTemplateRef}))

(def Volume
  (merge CommonAttrs
         {(s/optional-key :state)    volume-states
          :type                      NonBlankString
          :capacity                  PosInt
          (s/optional-key :bootable) s/Bool
          (s/optional-key :eventLog) NonBlankString
          }))

(def VolumeTemplateRef
  (merge VolumeTemplateAttrs
         {(s/optional-key :href) NonBlankString}))

(def VolumeCreate
  (merge CreateAttrs
         {:volumeTemplate VolumeTemplateRef}))

;;
;; Jobs
;;

(def job-states (s/enum "QUEUED" "RUNNING" "FAILED" "SUCCESS" "STOPPING" "STOPPED"))

(def Job
  (merge CommonAttrs
         {(s/optional-key :state)              job-states
          :targetResource                      NonBlankString
          (s/optional-key :affectedResources)  NonEmptyStrList
          :action                              NonBlankString
          (s/optional-key :returnCode)         s/Int
          (s/optional-key :progress)           NonNegInt
          (s/optional-key :statusMessage)      NonBlankString
          (s/optional-key :timeOfStatusChange) s/Inst
          (s/optional-key :parentJob)          NonBlankString
          (s/optional-key :nestedJobs)         NonEmptyStrList}))

;;
;; Event
;;

(def outcome-values (s/enum "Pending" "Unknown" "Status" "Success" "Warning" "Failure"))

(def severity-values (s/enum "critical" "high" "medium" "low"))

(def StateContent
  {:resName                   NonBlankString
   :resource                  NonBlankString
   :resType                   NonBlankString
   :state                     NonBlankString
   (s/optional-key :previous) NonBlankString})

(def AlarmContent
  {:resName                 NonBlankString
   :resource                NonBlankString
   :resType                 NonBlankString
   :code                    NonBlankString
   (s/optional-key :detail) NonBlankString})

(def ModelContent
  {:resName                 NonBlankString
   :resource                NonBlankString
   :resType                 NonBlankString
   :change                  NonBlankString
   (s/optional-key :detail) NonBlankString})

(def AccessContent
  {:operation               NonBlankString
   :resource                NonBlankString
   (s/optional-key :detail) NonBlankString
   :initiator               NonBlankString})

(def Event
  (merge CommonAttrs
         {:timestamp                s/Inst
          :type                     NonBlankString
          (s/optional-key :content) (s/either StateContent AlarmContent ModelContent AccessContent)
          :outcome                  outcome-values
          :severity                 severity-values
          (s/optional-key :contact) NonBlankString}))

;;
;; MachineConfiguration
;;

(def Disk
  {:capacity                         PosInt
   :format                           NonBlankString
   (s/optional-key :initialLocation) NonBlankString})

(def Disks
  (s/both [Disk] NotEmpty))

(def MachineConfiguration
  (merge CommonAttrs
         {:cpu                    PosInt
          :memory                 PosInt
          :cpuArch                ValidCpuArch
          (s/optional-key :disks) Disks}))

;;
;; MachineImage
;;

(def machine-image-state-values (s/enum "CREATING" "AVAILABLE" "DELETING" "ERROR"))

(def machine-image-type-values (s/enum "IMAGE" "SNAPSHOT" "PARTIAL_SNAPSHOT"))

(def MachineImage
  (merge CommonAttrs
         {:state                          machine-image-state-values
          :type                           machine-image-type-values
          (s/optional-key :imageLocation) NonBlankString
          (s/optional-key :relatedImage)  ResourceLink}))

;;
;; MachineTemplate
;;

(def machine-template-network-state-values (s/enum "Active", "Passive", "Disabled"))

(def MachineTemplateVolume
  {(s/optional-key :initialLocation) NonBlankString
   :volume                           ResourceLink})

(def MachineTemplateVolumes
  (s/both [MachineTemplateVolume] NotEmpty))

(def MachineTemplateVolumeTemplate
  {(s/optional-key :initialLocation) NonBlankString
   :volumeTemplate                   ResourceLink})

(def MachineTemplateVolumeTemplates
  (s/both [MachineTemplateVolumeTemplate] NotEmpty))

(def MachineTemplateAddresses
  (s/both [ResourceLink] NotEmpty))

(def MachineTemplateNetworkInterface
  {(s/optional-key :addresses) MachineTemplateAddresses
   :network                    ResourceLink
   :networkPort                ResourceLink
   :state                      machine-template-network-state-values
   :mtu                        PosInt})

(def MachineTemplateNetworkInterfaces
  (s/both [MachineTemplateNetworkInterface] NotEmpty))

(def MachineTemplate
  (merge CommonAttrs
         {:initialState                       NonBlankString
          :machineConfig                      ResourceLink
          :machineImage                       ResourceLink
          (s/optional-key :credential)        ResourceLink
          (s/optional-key :volumes)           MachineTemplateVolumes
          (s/optional-key :volumeTemplates)   MachineTemplateVolumeTemplates
          (s/optional-key :networkInterfaces) MachineTemplateNetworkInterfaces
          (s/optional-key :userData)          NonBlankString
          (s/optional-key :meterTemplates)    ResourceLinks
          (s/optional-key :eventLogTemplate)  ResourceLink}))

;;
;; Machine
;;

(def machine-template-state-values (s/enum "CREATING"
                                           "STARTING" "STARTED"
                                           "STOPPING" "STOPPED"
                                           "PAUSING" "PAUSED"
                                           "SUSPENDING" "SUSPENDED"
                                           "DELETING" "ERROR"))

(def MachineTemplate
  (merge CommonAttrs
         {:state                              machine-template-network-state-values
          :cpu                                PosInt
          :memory                             PosInt
          (s/optional-key :disks)             ResourceLink
          :cpuArch                            ValidCpuArch
          (s/optional-key :volumes)           ResourceLink
          (s/optional-key :networkInterfaces) ResourceLink
          (s/optional-key :latestSnapshot)    ResourceLink
          (s/optional-key :snapshots)         ResourceLinks
          (s/optional-key :meters)            ResourceLinks
          (s/optional-key :eventLog)          ResourceLink}))

;;
;; ServiceMessage
;;

(def ServiceMessage
  (merge CommonAttrs
         {:title   NonBlankString
          :message NonBlankString}))
