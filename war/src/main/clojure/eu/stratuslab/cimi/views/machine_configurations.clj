(ns eu.stratuslab.cimi.views.machine-configurations
  "Utilities for managing the CRUD features for machine configurations."
  (:import [java.util UUID])
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :refer [debug info error]]
            [clj-hector.core :refer [put get-rows delete-rows get-rows-cql-query]]))

(def ^:const ks-name "stratuslab_cimi")

(def ^:const cf-name "machine_templates")

(def ^:const column-metadata [{:name "id" :validator :utf-8}
                              {:name "name" :validator :utf-8}
                              {:name "description" :validator :utf-8}
                              {:name "created" :validator :long}
                              {:name "updated" :validator :long}
                              {:name "cpu" :validator :integer}
                              {:name "memory" :validator :integer}
                              {:name "cpuArch" :validator :utf-8}])

(def ^:const serializer-opts [:k-serializer :string
                              :n-serializer :string
                              :v-serializer :string])
