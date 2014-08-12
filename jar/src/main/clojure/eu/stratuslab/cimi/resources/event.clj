;
; Copyright 2013 Centre National de la Recherche Scientifique (CNRS)
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
(ns eu.stratuslab.cimi.resources.event
  "Resources containing information about service events concerning
   specific resources."
  (:require
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [ring.util.response :as r]
    [schema.core :as s]
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [cemerick.friend :as friend]))

(def ^:const resource-tag :events)

(def ^:const resource-name "Event")

(def ^:const collection-name "EventCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; Event
;;

(def outcome-values (s/enum "Pending" "Unknown" "Status" "Success" "Warning" "Failure"))

(def severity-values (s/enum "critical" "high" "medium" "low"))

(def StateContent
  {:resName                   c/NonBlankString
   :resource                  c/NonBlankString
   :resType                   c/NonBlankString
   :state                     c/NonBlankString
   (s/optional-key :previous) c/NonBlankString})

(def AlarmContent
  {:resName                 c/NonBlankString
   :resource                c/NonBlankString
   :resType                 c/NonBlankString
   :code                    c/NonBlankString
   (s/optional-key :detail) c/NonBlankString})

(def ModelContent
  {:resName                 c/NonBlankString
   :resource                c/NonBlankString
   :resType                 c/NonBlankString
   :change                  c/NonBlankString
   (s/optional-key :detail) c/NonBlankString})

(def AccessContent
  {:operation               c/NonBlankString
   :resource                c/NonBlankString
   (s/optional-key :detail) c/NonBlankString
   :initiator               c/NonBlankString})

;; FIXME: Determine the actual schema to be used for audit events.
(def CADFContent
  (s/both
    {s/Keyword s/Any}
    c/NotEmpty))

(def CommonEventAttrs
  {:timestamp                c/Timestamp
   :outcome                  outcome-values
   :severity                 severity-values
   (s/optional-key :contact) c/NonBlankString})

(def Event
  (s/either
    (merge c/CommonAttrs c/AclAttr CommonEventAttrs
           {:type (s/eq "state") :content StateContent})
    (merge c/CommonAttrs c/AclAttr CommonEventAttrs
           {:type (s/eq "alarm") :content AlarmContent})
    (merge c/CommonAttrs c/AclAttr CommonEventAttrs
           {:type (s/eq "model") :content ModelContent})
    (merge c/CommonAttrs c/AclAttr CommonEventAttrs
           {:type (s/eq "access") :content AccessContent})
    (merge c/CommonAttrs c/AclAttr CommonEventAttrs
           {:type #"http://schemas\.dmtf\.org/cloud/audit/1\.0/.*" :content CADFContent})))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Event))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (a/add-acl resource (friend/current-authentication)))

;;
;; CRUD operations
;;

(def add-impl (crud/get-add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
           [request]
  (add-impl request))

(def retrieve-impl (crud/get-retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
           [request]
  (retrieve-impl request))

(def edit-impl (crud/get-edit-fn resource-name))

(defmethod crud/edit resource-name
           [request]
  (edit-impl request))

(def delete-impl (crud/get-delete-fn resource-name))

(defmethod crud/delete resource-name
           [request]
  (delete-impl request))

(def query-impl (crud/get-query-fn resource-name collection-acl collection-uri collection-name resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))
