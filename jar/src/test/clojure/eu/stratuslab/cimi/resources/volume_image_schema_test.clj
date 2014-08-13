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

(ns eu.stratuslab.cimi.resources.volume-image-schema-test
  (:require
    [eu.stratuslab.cimi.resources.volume-image :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.common.schema :as c]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def valid-entry
  {:state         "CREATING"
   :imageLocation {:href "http://example.org/image/location"}
   :bootable      true})

(let [uri (str resource-name "/" (u/random-uuid))
      image (assoc valid-entry
              :acl valid-acl
              :id uri
              :resourceURI resource-uri
              :created "1964-08-25T10:00:00.0Z"
              :updated "1964-08-25T10:00:00.0Z")]

  (expect image (c/validate image))
  (expect Exception (c/validate (assoc valid-entry :invalid "BAD")))

  (expect nil? (s/check VolumeImage image))
  (expect (s/check VolumeImage (dissoc image :state)))
  (expect (s/check VolumeImage (dissoc image :imageLocation)))
  (expect (s/check VolumeImage (dissoc image :bootable)))
  (expect (s/check VolumeImage (assoc image :invalid "BAD")))
  (expect (s/check VolumeImage (assoc image :state "INVALID-STATE")))
  (expect (s/check VolumeImage (assoc image :imageLocation "1")))
  (expect (s/check VolumeImage (assoc image :bootable 1))))

(expect nil? (s/check VolumeImageRef valid-entry))
(expect nil? (s/check VolumeImageRef (assoc valid-entry :href "http://example.org/template")))
(expect nil? (s/check VolumeImageRef (dissoc valid-entry :state)))
(expect nil? (s/check VolumeImageRef (dissoc valid-entry :imageLocation)))
(expect nil? (s/check VolumeImageRef (dissoc valid-entry :bootable)))
(expect (s/check VolumeImageRef {}))
(expect (s/check VolumeImageRef (assoc valid-entry :invalid "BAD")))
(expect (s/check VolumeImageRef (assoc valid-entry :state "INVALID-STATE")))
(expect (s/check VolumeImageRef (assoc valid-entry :imageLocation "1")))
(expect (s/check VolumeImageRef (assoc valid-entry :bootable 1)))


(run-tests [*ns*])

