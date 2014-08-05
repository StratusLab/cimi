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

(ns eu.stratuslab.cimi.resources.meter-template-schema-test
  (:require
    [eu.stratuslab.cimi.resources.meter-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [eu.stratuslab.cimi.resources.utils.utils :as utils]))

(expect nil? (s/check MeterTemplateRef {:href "http://example.org/template"}))
(expect (s/check MeterTemplateRef {:invalid "BAD"}))
(expect (s/check MeterTemplateRef {}))

(expect nil? (s/check MeterTemplateRefs [{:href "http://example.org/template"}]))
(expect nil? (s/check MeterTemplateRefs [{:href "http://example.org/template"}
                                         {:href "http://example.org/template"}]))
(expect (s/check MeterTemplateRefs [{:invalid "BAD"}]))
(expect (s/check MeterTemplateRefs []))


(run-tests [*ns*])

