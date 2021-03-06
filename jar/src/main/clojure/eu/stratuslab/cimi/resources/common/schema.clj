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

(ns eu.stratuslab.cimi.resources.common.schema
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [schema.core :as s]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]))

(def ^:const cimi-schema-uri "http://schemas.dmtf.org/cimi/1/")

(def ^:const stratuslab-cimi-schema-uri "http://stratuslab.eu/cimi/1/")

(def ^:const service-context "/cimi/")

;;
;; actions
;;

(def ^:const valid-actions
  #{:add :edit :delete
    :start :stop :restart :pause :suspend
    :export :import :capture :snapshot})

(def ^:const action-uri
  (let [root "http://schemas.dmtf.org/cimi/1/Action/"
        m (into {} (map (fn [k] [k (str root (name k))]) valid-actions))]
    (assoc m :add "add" :edit "edit" :delete "delete")))

(def ^:const valid-action-uris
  (vals action-uri))

;;
;; schema definitions for basic types
;;

(def NotEmpty
  (s/pred seq "not-empty?"))

(def PosInt
  (s/both s/Int (s/pred pos? "pos?")))

(def NonNegInt
  (s/both s/Int (s/pred (complement neg?) "not-neg?")))

(def NonBlankString
  (s/both s/Str (s/pred (complement str/blank?) "not-blank?")))

(def NonEmptyStrList
  (s/both [NonBlankString] NotEmpty))

(def Timestamp
  (s/both NonBlankString (s/pred u/valid-timestamp? "valid-timestamp?")))

;;
;; schema definitions for common attributes
;;

(def ResourceLink
  {:href NonBlankString})

(def ResourceLinks
  (s/both [ResourceLink] NotEmpty))

(def Operation
  (merge ResourceLink {:rel NonBlankString}))

(def Operations
  (s/both [Operation] NotEmpty))

(def Properties
  (s/both
    {(s/either s/Keyword s/Str) s/Str}
    NotEmpty))

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; StratusLab implementation.
;;

(def access-control-types (s/enum "USER" "ROLE"))

(def access-control-rights (s/enum "ALL" "VIEW" "MODIFY"))

(def AccessControlId
  {:principal NonBlankString
   :type      access-control-types})

(def AccessControlRule
  (merge AccessControlId {:right access-control-rights}))

(def AccessControlRules
  (s/both [AccessControlRule] NotEmpty))

(def AccessControlList
  {:owner                  AccessControlId
   (s/optional-key :rules) AccessControlRules})

;;
;; Common Attributes
;;

;;
;; These attributes are common to all resources except the
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;
(def CommonAttrs
  {:id                           NonBlankString
   :resourceURI                  NonBlankString
   (s/optional-key :name)        NonBlankString
   (s/optional-key :description) NonBlankString
   :created                      Timestamp
   :updated                      Timestamp
   (s/optional-key :properties)  Properties
   (s/optional-key :operations)  Operations})

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(def CreateAttrs
  {:resourceURI                  NonBlankString
   (s/optional-key :name)        NonBlankString
   (s/optional-key :description) NonBlankString
   (s/optional-key :created)     Timestamp
   (s/optional-key :updated)     Timestamp
   (s/optional-key :properties)  Properties
   (s/optional-key :operations)  Operations})

;;
;; Ownership and access control (StratusLab extension)
;;
;; These are additions to the standard CIMI schema for the
;; StratusLab implementation.
;;

(def access-control-types (s/enum "USER" "ROLE"))

(def access-control-rights (s/enum "ALL" "VIEW" "MODIFY"))

(def AccessControlId
  {:principal NonBlankString
   :type      access-control-types})

(def AccessControlRule
  (merge AccessControlId {:right access-control-rights}))

(def AccessControlRules
  (s/both [AccessControlRule] NotEmpty))

(def AccessControlList
  {:owner                  AccessControlId
   (s/optional-key :rules) AccessControlRules})

(def AclAttr
  {:acl AccessControlList})

;;
;; common resource utilities and multimethods
;;

(defn get-resource-typeuri
  "This will return the resource typeURI corresponding to the given
   resource template typeURI by removing 'Template' at the end of the
   URI.  Returns modified value or nil if nil was passed in."
  [template-typeuri]
  (if template-typeuri
    (->> template-typeuri
         (re-matches #"^(.*?)(?:Template)?(#.*)?$")
         (rest)
         (apply str))))

(defmulti template->resource
          "Converts a resource template to an instance of the resource.  This
           dispatches on the value :resourceURI.  The default implementation
           copies the template with a modified :resourceURI field.  The method removes
           'Template' from the end of the original :resourceURI value."
          :resourceURI)

(defmethod template->resource :default
           [{:keys [resourceURI] :as template}]
  (if-let [resource-typeuri (get-resource-typeuri resourceURI)]
    (assoc template :resourceURI resource-typeuri)
    template))


(defmulti validate-template
          "Validates the given resource template, returning the template itself on success.
           This method dispatches on the value of resourceURI.  For any unknown
           dispatch value, the method throws an exception."
          :resourceURI)

(defmethod validate-template :default
           [template]
  (throw (ex-info (str "unknown resource type: " (:resourceURI template)) template)))


(defmulti validate
          "Validates the given resource, returning the resource itself on success.
           This method dispatches on the value of resourceURI.  For any unknown
           dispatch value, the method throws an exception."
          :resourceURI)

(defmethod validate :default
           [resource]
  (throw (ex-info (str "unknown resource type: " (:resourceURI resource)) resource)))

(defmulti set-operations
          "Adds the authorized resource operations to the resource based on the current
           user and the resource's ACL.  Dispatches on the value of resourceURI.
           For any unregistered resourceURI, the default implementation will add the
           'add' action for a Collection and the 'edit' and 'delete' actions for resources,
           if the current user has the MODIFY right."
          :resourceURI)

(defmethod set-operations :default
           [{:keys [id resourceURI] :as resource} request]
  (try
    (a/modifiable? resource request)
    (let [ops (if (.endsWith resourceURI "Collection")
                [{:rel (:add action-uri) :href id}]
                [{:rel (:edit action-uri) :href id}
                 {:rel (:delete action-uri) :href id}])]
      (assoc resource :operations ops))
    (catch Exception e
      (dissoc resource :operations))))

