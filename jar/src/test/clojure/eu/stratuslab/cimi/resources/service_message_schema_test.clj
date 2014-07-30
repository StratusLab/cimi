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

(ns eu.stratuslab.cimi.resources.service-message-schema-test
  (:require
    [eu.stratuslab.cimi.resources.service-message :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp #inst "1964-08-25T10:00:00.0Z"
      uri (uuid->id timestamp)
      sm {:acl         valid-acl
          :id          uri
          :resourceURI resource-uri
          :created     timestamp
          :updated     timestamp
          :title       "title"
          :message     "message"}]

  (expect nil? (s/check ServiceMessage sm))
  (expect (s/check ServiceMessage (dissoc sm :title)))
  (expect (s/check ServiceMessage (dissoc sm :message))))


(run-tests [*ns*])

