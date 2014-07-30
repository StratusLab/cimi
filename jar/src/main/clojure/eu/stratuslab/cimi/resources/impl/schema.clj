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
    [schema.core :as s]
    [eu.stratuslab.cimi.resources.impl.common :as c]))

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

(def ValidCpuArch
  (s/enum "68000" "Alpha" "ARM" "Itanium" "MIPS" "PA_RISC"
          "POWER" "PowerPC" "x86" "x86_64" "zArchitecture", "SPARC"))

;;
;; User records within the database.  (StratusLab extension.)
;;
(def Roles
  (s/both
    [c/NonBlankString]
    c/NotEmpty))

(def Altnames
  (s/both
    {s/Keyword c/NonBlankString}
    c/NotEmpty))

(def User
  (merge c/CommonAttrs
         c/AclAttr
         {:first-name                c/NonBlankString
          :last-name                 c/NonBlankString
          :username                  c/NonBlankString
          (s/optional-key :password) c/NonBlankString
          (s/optional-key :enabled)  s/Bool
          (s/optional-key :roles)    Roles
          (s/optional-key :altnames) Altnames
          (s/optional-key :email)    c/NonBlankString}))

;;
;; Service configuration files.  (StratusLab extension.)
;;
(def ServiceConfiguration
  (merge c/CommonAttrs
         c/AclAttr
         {:service                   c/NonBlankString
          (s/optional-key :instance) c/NonBlankString}))

;;
;; Cloud Entry Point Schema
;;

(def CloudEntryPoint
  (merge c/CommonAttrs
         c/AclAttr
         {:baseURI                                   c/NonBlankString
          (s/optional-key :resourceMetadata)         c/ResourceLink
          (s/optional-key :systems)                  c/ResourceLink
          (s/optional-key :systemTemplates)          c/ResourceLink
          (s/optional-key :machines)                 c/ResourceLink
          (s/optional-key :machineTemplates)         c/ResourceLink
          (s/optional-key :machineConfigs)           c/ResourceLink
          (s/optional-key :machineImages)            c/ResourceLink
          (s/optional-key :credentials)              c/ResourceLink
          (s/optional-key :credentialTemplates)      c/ResourceLink
          (s/optional-key :volumes)                  c/ResourceLink
          (s/optional-key :volumeTemplates)          c/ResourceLink
          (s/optional-key :volumeConfigs)            c/ResourceLink
          (s/optional-key :volumeImages)             c/ResourceLink
          (s/optional-key :networks)                 c/ResourceLink
          (s/optional-key :networkTemplates)         c/ResourceLink
          (s/optional-key :networkConfigs)           c/ResourceLink
          (s/optional-key :networkPorts)             c/ResourceLink
          (s/optional-key :networkPortTemplates)     c/ResourceLink
          (s/optional-key :networkPortConfigs)       c/ResourceLink
          (s/optional-key :addresses)                c/ResourceLink
          (s/optional-key :addressTemplates)         c/ResourceLink
          (s/optional-key :forwardingGroups)         c/ResourceLink
          (s/optional-key :forwardingGroupTemplates) c/ResourceLink
          (s/optional-key :jobs)                     c/ResourceLink
          (s/optional-key :meters)                   c/ResourceLink
          (s/optional-key :meterTemplates)           c/ResourceLink
          (s/optional-key :meterConfigs)             c/ResourceLink
          (s/optional-key :eventLogs)                c/ResourceLink
          (s/optional-key :eventLogTemplates)        c/ResourceLink
          (s/optional-key :serviceConfigurations)    c/ResourceLink ;; StratusLab extension
          (s/optional-key :serviceMessages)          c/ResourceLink ;; StratusLab extension
          (s/optional-key :users)                    c/ResourceLink ;; StratusLab extension
          }))

;;
;; Volume Related Schemas
;;

(def image-states (s/enum "CREATING" "AVAILABLE" "DELETING" "ERROR"))

(def volume-states (s/enum "CREATING" "AVAILABLE" "CAPTURING" "DELETING" "ERROR"))

(def VolumeConfigurationAttrs
  {(s/optional-key :type)     c/NonBlankString
   (s/optional-key :format)   c/NonBlankString
   (s/optional-key :capacity) c/PosInt})

(def VolumeConfigurationAttrs
  {(s/optional-key :type)     c/NonBlankString
   (s/optional-key :format)   c/NonBlankString
   (s/optional-key :capacity) c/PosInt})

(def VolumeConfiguration
  (merge c/CommonAttrs
         c/AclAttr
         {(s/optional-key :type)   c/NonBlankString
          (s/optional-key :format) c/NonBlankString
          :capacity                c/PosInt}))

(def VolumeImageAttrs
  {(s/optional-key :state)         image-states
   (s/optional-key :imageLocation) c/ResourceLink
   (s/optional-key :bootable)      s/Bool})

(def VolumeImage
  (merge c/CommonAttrs
         c/AclAttr
         {:state         image-states
          :imageLocation c/ResourceLink
          :bootable      s/Bool}))

(def VolumeConfigurationRef
  (merge VolumeConfigurationAttrs
         {(s/optional-key :href) c/NonBlankString}))

(def VolumeImageRef
  (merge VolumeImageAttrs
         {(s/optional-key :href) c/NonBlankString}))

;; TODO: Add real schema once Meters are supported.
(def MeterTemplateRef
  {})

(def MeterTemplateRefs
  (s/both [MeterTemplateRef] c/NotEmpty))

;; TODO: Add real schema once EventLogs are supported.
(def EventLogTemplateRef
  {})

(def VolumeTemplateAttrs
  {(s/optional-key :volumeConfig)     VolumeConfigurationRef
   (s/optional-key :volumeImage)      VolumeImageRef
   (s/optional-key :meterTemplates)   MeterTemplateRefs
   (s/optional-key :eventLogTemplate) EventLogTemplateRef})

(def VolumeTemplate
  (merge c/CommonAttrs
         c/AclAttr
         {:volumeConfig                      VolumeConfigurationRef
          (s/optional-key :volumeImage)      VolumeImageRef
          (s/optional-key :meterTemplates)   MeterTemplateRefs
          (s/optional-key :eventLogTemplate) EventLogTemplateRef}))

(def Volume
  (merge c/CommonAttrs
         c/AclAttr
         {(s/optional-key :state)    volume-states
          :type                      c/NonBlankString
          :capacity                  c/PosInt
          (s/optional-key :bootable) s/Bool
          (s/optional-key :eventLog) c/NonBlankString
          }))

(def VolumeTemplateRef
  (merge VolumeTemplateAttrs
         {(s/optional-key :href) c/NonBlankString}))

(def VolumeCreate
  (merge c/CreateAttrs
         c/AclAttr
         {:volumeTemplate VolumeTemplateRef}))

;;
;; Jobs
;;

(def job-states (s/enum "QUEUED" "RUNNING" "FAILED" "SUCCESS" "STOPPING" "STOPPED"))

(def Job
  (merge c/CommonAttrs
         c/AclAttr
         {(s/optional-key :state)              job-states
          :targetResource                      c/NonBlankString
          (s/optional-key :affectedResources)  c/NonEmptyStrList
          :action                              c/NonBlankString
          (s/optional-key :returnCode)         s/Int
          (s/optional-key :progress)           c/NonNegInt
          (s/optional-key :statusMessage)      c/NonBlankString
          (s/optional-key :timeOfStatusChange) s/Inst
          (s/optional-key :parentJob)          c/NonBlankString
          (s/optional-key :nestedJobs)         c/NonEmptyStrList}))

;;
;; Event
;;

(def outcome-values (s/enum "Pending" "Unknown" "Status" "Success" "Warning" "Failure"))

(def severity-values (s/enum "critical" "high" "medium" "low"))

(def StateContent
  {:resName                   c/NonBlankString
   :resource                  c/NonBlankString
   :resType                   c/NonBlankString
   :state                     c/NonBlankString
   (s/optional-key :previous) c/NonBlankString})

(def AlarmContent
  {:resName                 c/NonBlankString
   :resource                c/NonBlankString
   :resType                 c/NonBlankString
   :code                    c/NonBlankString
   (s/optional-key :detail) c/NonBlankString})

(def ModelContent
  {:resName                 c/NonBlankString
   :resource                c/NonBlankString
   :resType                 c/NonBlankString
   :change                  c/NonBlankString
   (s/optional-key :detail) c/NonBlankString})

(def AccessContent
  {:operation               c/NonBlankString
   :resource                c/NonBlankString
   (s/optional-key :detail) c/NonBlankString
   :initiator               c/NonBlankString})

(def Event
  (merge c/CommonAttrs
         c/AclAttr
         {:timestamp                s/Inst
          :type                     c/NonBlankString
          (s/optional-key :content) (s/either StateContent AlarmContent ModelContent AccessContent)
          :outcome                  outcome-values
          :severity                 severity-values
          (s/optional-key :contact) c/NonBlankString}))

;;
;; MachineConfiguration
;;

(def Disk
  {:capacity                         c/PosInt
   :format                           c/NonBlankString
   (s/optional-key :initialLocation) c/NonBlankString})

(def Disks
  (s/both [Disk] c/NotEmpty))

(def MachineConfiguration
  (merge c/CommonAttrs
         c/AclAttr
         {:cpu                    c/PosInt
          :memory                 c/PosInt
          :cpuArch                ValidCpuArch
          (s/optional-key :disks) Disks}))

;;
;; MachineImage
;;

(def machine-image-state-values (s/enum "CREATING" "AVAILABLE" "DELETING" "ERROR"))

(def machine-image-type-values (s/enum "IMAGE" "SNAPSHOT" "PARTIAL_SNAPSHOT"))

(def MachineImage
  (merge c/CommonAttrs
         c/AclAttr
         {:state                          machine-image-state-values
          :type                           machine-image-type-values
          (s/optional-key :imageLocation) c/NonBlankString
          (s/optional-key :relatedImage)  c/ResourceLink}))

;;
;; MachineTemplate
;;

(def machine-template-network-state-values (s/enum "Active", "Passive", "Disabled"))

(def MachineTemplateVolume
  {(s/optional-key :initialLocation) c/NonBlankString
   :volume                           c/ResourceLink})

(def MachineTemplateVolumes
  (s/both [MachineTemplateVolume] c/NotEmpty))

(def MachineTemplateVolumeTemplate
  {(s/optional-key :initialLocation) c/NonBlankString
   :volumeTemplate                   c/ResourceLink})

(def MachineTemplateVolumeTemplates
  (s/both [MachineTemplateVolumeTemplate] c/NotEmpty))

(def MachineTemplateAddresses
  (s/both [c/ResourceLink] c/NotEmpty))

(def MachineTemplateNetworkInterface
  {(s/optional-key :addresses) MachineTemplateAddresses
   :network                    c/ResourceLink
   :networkPort                c/ResourceLink
   :state                      machine-template-network-state-values
   :mtu                        c/PosInt})

(def MachineTemplateNetworkInterfaces
  (s/both [MachineTemplateNetworkInterface] c/NotEmpty))

(def MachineTemplate
  (merge c/CommonAttrs
         c/AclAttr
         {:initialState                       c/NonBlankString
          :machineConfig                      c/ResourceLink
          :machineImage                       c/ResourceLink
          (s/optional-key :credential)        c/ResourceLink
          (s/optional-key :volumes)           MachineTemplateVolumes
          (s/optional-key :volumeTemplates)   MachineTemplateVolumeTemplates
          (s/optional-key :networkInterfaces) MachineTemplateNetworkInterfaces
          (s/optional-key :userData)          c/NonBlankString
          (s/optional-key :meterTemplates)    c/ResourceLinks
          (s/optional-key :eventLogTemplate)  c/ResourceLink}))

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
  (merge c/CommonAttrs
         c/AclAttr
         {:state                              machine-template-network-state-values
          :cpu                                c/PosInt
          :memory                             c/PosInt
          (s/optional-key :disks)             c/ResourceLink
          :cpuArch                            ValidCpuArch
          (s/optional-key :volumes)           c/ResourceLink
          (s/optional-key :networkInterfaces) c/ResourceLink
          (s/optional-key :latestSnapshot)    c/ResourceLink
          (s/optional-key :snapshots)         c/ResourceLinks
          (s/optional-key :meters)            c/ResourceLinks
          (s/optional-key :eventLog)          c/ResourceLink}))

;;
;; ServiceMessage
;;

(def ServiceMessage
  (merge c/CommonAttrs
         c/AclAttr
         {:title   c/NonBlankString
          :message c/NonBlankString}))
