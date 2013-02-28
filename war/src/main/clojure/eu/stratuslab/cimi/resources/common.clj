(ns eu.stratuslab.cimi.resources.common
  "Data, definitions, and utilities common to all resources."
  (:import [java.util UUID]
           [java.nio ByteBuffer])
  (:require [eu.stratuslab.cimi.resources.utils :as utils]
            [clojure.walk :as walk]
            [clojure.tools.logging :refer [debug info error]]
            [clj-hector.core :refer [put get-rows get-rows-cql-query delete-rows]]
            [clj-hector.serialize :as serial]))

(def ^:const resource-root-uri "http://www.dmtf.org/cimi/")

(def attributes
  "Set of the attributes allowed for all CIMI resources except
   resourceMetadata."
  #{:id :name :description :created :updated :properties})

(def immutable-attributes
  "Set of the common attributes that cannot be modified when 
   updating a resource."
  #{:id :created :updated})

(def ^:const common-resource-attrs
     [{:name "id"
       :validator :utf-8
       :serializer :string}
      {:name "name"
       :validator :utf-8
       :serializer :string}
      {:name "description"
       :validator :utf-8
       :serializer :string}
      {:name "created"
       :validator :long
       :serializer :date}
      {:name "updated"
       :validator :long
       :serializer :date}])

(defn add-common-resource-attrs
  [attrs]
  (concat common-resource-attrs attrs))

(defn attrs->serializer-map
  [attrs]
  (into {} (map (juxt #(keyword (:name %)) :serializer) attrs)))

(defn create-value-serializer-function
  "Creates a value serializer function from a map of attribute
  names (as keywords) and serializer names (also as keywords).
  The :string serializer will be used for any column that does not
  have an explicit serializer.  The input of the returned function is
  a vector containing a key-value pair.  If the value is not a byte
  array, then it is returned unmodified."
  [serializer-map]
  (fn [[key bytes]]
    (if (instance? (Class/forName "[B") bytes)
      (if-let [serializer-name (or (get serializer-map key) :string)]
        (let [s (serial/serializer serializer-name)]
          [key (.fromBytes s bytes)])
        [key bytes])
      [key bytes])))

(defn create-serialize-values-function
  "Returns a function that recursively transforms all map values from
  byte arrays to clojure values using a serializer based on the key.
  The map provides the mapping between the column name (as a keyword)
  and the serializer (also as a keyword)."
  [attrs]
  (let [serializer-map (attrs->serializer-map attrs)
        f (create-value-serializer-function serializer-map)]
    (fn [data]
      (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) data))))

