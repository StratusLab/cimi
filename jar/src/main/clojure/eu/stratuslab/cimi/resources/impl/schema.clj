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

(def ValidCpuArch
  (s/enum "68000" "Alpha" "ARM" "Itanium" "MIPS" "PA_RISC"
          "POWER" "PowerPC" "x86" "x86_64" "zArchitecture", "SPARC"))

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

