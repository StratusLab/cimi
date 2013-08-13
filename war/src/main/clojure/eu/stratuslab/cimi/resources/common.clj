(ns eu.stratuslab.cimi.resources.common
  "Data, definitions, and utilities common to all resources."
  (:require
    [clojure.tools.logging :refer [debug info error]]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]))

(def ^:const resource-uri "http://schemas.dmtf.org/cimi/1/")

(def attributes
  "Set of the attributes allowed for all CIMI resources except
   resourceMetadata."
  #{:id :name :description :created :updated :properties})

(def immutable-attributes
  "Set of the common attributes that cannot be modified when 
   updating a resource."
  #{:id :created :updated})

(def-map-schema PropertyMap
  (constraints (fn [m] (pos? (count (keys m)))))
  [[(wild NonEmptyString)] String])

(def-map-schema common-attrs-schema
  [[:id] NonEmptyString
   (optional-path [:name]) NonEmptyString
   (optional-path [:description]) NonEmptyString
   [:created] NonEmptyString
   [:updated] NonEmptyString
   (optional-path [:properties]) PropertyMap])


