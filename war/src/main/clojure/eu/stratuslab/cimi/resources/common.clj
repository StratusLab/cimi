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

(def-map-schema ^{:doc "Link to another resource."} ResourceLink
                [[:href] NonEmptyString])

(def-map-schema Operation
                [[:rel] (set (vals action-uri))
                 [:href] NonEmptyString])

(def-seq-schema Operations
                (constraints (fn [s] (pos? (count s))))
                [Operation])

(def-map-schema Properties
                (constraints (fn [m] (pos? (count (keys m)))))
                [[(wild NonEmptyString)] String])

;;
;; These attributes are common to all resources except the 
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;
(def-map-schema CommonAttrs
                [[:id] NonEmptyString
                 [:resourceURI] NonEmptyString
                 (optional-path [:name]) NonEmptyString
                 (optional-path [:description]) NonEmptyString
                 [:created] NonEmptyString
                 [:updated] NonEmptyString
                 (optional-path [:properties]) Properties
                 (optional-path [:operations]) Operations])

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(def-map-schema CreateAttrs
                [[:resourceURI] NonEmptyString
                 (optional-path [:name]) NonEmptyString
                 (optional-path [:description]) NonEmptyString
                 (optional-path [:created]) NonEmptyString
                 (optional-path [:updated]) NonEmptyString
                 (optional-path [:properties]) Properties
                 (optional-path [:operations]) Operations])
