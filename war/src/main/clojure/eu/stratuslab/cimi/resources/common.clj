(ns eu.stratuslab.cimi.resources.common
  "Data, definitions, and utilities common to all resources."
  (:require [clojure.tools.logging :refer [debug info error]]))

(def ^:const resource-uri "http://schemas.dmtf.org/cimi/1/")

(def attributes
  "Set of the attributes allowed for all CIMI resources except
   resourceMetadata."
  #{:id :name :description :created :updated :properties})

(def immutable-attributes
  "Set of the common attributes that cannot be modified when 
   updating a resource."
  #{:id :created :updated})
