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

(ns eu.stratuslab.cimi.resources.cloud-entry-point-schema-test
  (:require
    [eu.stratuslab.cimi.resources.cloud-entry-point :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(let [timestamp #inst "1964-08-25T10:00:00.0Z"
      cep {:id          resource-name
           :resourceURI base-uri
           :created     timestamp
           :updated     timestamp
           :acl         resource-acl
           :baseURI     "http://cloud.example.org/"}]

  (expect nil? (s/check CloudEntryPoint cep))
  (expect nil? (s/check CloudEntryPoint (assoc cep :resources {:href "Resource/uuid"})))
  (expect (s/check CloudEntryPoint (dissoc cep :created)))
  (expect (s/check CloudEntryPoint (dissoc cep :updated)))
  (expect (s/check CloudEntryPoint (dissoc cep :baseURI)))
  (expect (s/check CloudEntryPoint (dissoc cep :acl))))


(run-tests [*ns*])

