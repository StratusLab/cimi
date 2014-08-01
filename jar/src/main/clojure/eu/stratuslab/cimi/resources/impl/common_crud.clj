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

(ns eu.stratuslab.cimi.resources.impl.common-crud
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [schema.core :as s]))

(defmulti add
          (fn [& args]
            (first args)))

(defmethod add :default
           [& args]
  (u/bad-method))


(defmulti query
          (fn [& args]
            (first args)))

(defmethod query :default
           [& args]
  (u/bad-method))


(defmulti retrieve
          (fn [& args]
            (first args)))

(defmethod retrieve :default
           [& args]
  (u/bad-method))


(defmulti edit
          (fn [& args]
            (first args)))

(defmethod edit :default
           [& args]
  (u/bad-method))


(defmulti delete
          (fn [& args]
            (first args)))

(defmethod delete :default
           [& args]
  (u/bad-method))
