(ns eu.stratuslab.cimi.resources.common
  "Data, definitions, and utilities common to all resources."
  (:require
    [clojure.tools.logging :refer [debug info error]]
    [clj-schema.schema :refer :all]
    [clj-schema.simple-schemas :refer :all]
    [clj-schema.validation :refer :all]))

(def ^:const resource-uri "http://schemas.dmtf.org/cimi/1/")

(def ^:const valid-actions
  #{:add :edit :delete
    :start :stop :restart :pause :suspend
    :export :import :capture :snapshot})

(def ^:const action-uri
  (let [root "http://schemas.dmtf.org/cimi/1/Action/"
        m (into {} (map (fn [k] [k (str root (name k))]) valid-actions))]
    (assoc m :add "add" :edit "edit" :delete "delete")))

(def-map-schema Operation
  [[:rel] (set (vals action-uri))
   [:href] NonEmptyString])

(def-seq-schema Operations
  (constraints (fn [s] (pos? (count s))))
  [Operation])

(def-map-schema Properties
  (constraints (fn [m] (pos? (count (keys m)))))
  [[(wild NonEmptyString)] String])

(def-map-schema CommonAttrs
  [[:id] NonEmptyString
   [:resourceURI] NonEmptyString
   (optional-path [:name]) NonEmptyString
   (optional-path [:description]) NonEmptyString
   [:created] NonEmptyString
   [:updated] NonEmptyString
   (optional-path [:properties]) Properties
   (optional-path [:operations]) Operations])
