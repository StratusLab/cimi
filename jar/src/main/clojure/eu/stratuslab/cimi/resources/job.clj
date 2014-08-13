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
(ns eu.stratuslab.cimi.resources.job
  "Utilities for managing the CRUD features for jobs."
  (:require
    [eu.stratuslab.cimi.resources.impl.common-crud :as crud]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [ring.util.response :as r]
    [clojure.walk :as w]
    [schema.core :as s]
    [clojure.tools.logging :as log]
    [cemerick.friend :as friend]
    [eu.stratuslab.cimi.db.dbops :as db]))

(def ^:const resource-tag :jobs)

(def ^:const resource-name "Job")

(def ^:const collection-name "JobCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;; FIXME: This ACL should depend on who is accessing the object
(def job-acl {:owner {:principal "::ADMIN" :type "ROLE"}})

(defn uuid->uri
  "Convert a uuid into the URI for a MachineConfiguration resource.
   The URI must not have a leading slash."
  [uuid]
  (str resource-name "/" uuid))

;;
;; schema
;;

(def job-states (s/enum "QUEUED" "RUNNING" "FAILED" "SUCCESS" "STOPPING" "STOPPED"))

(def Job
  (merge c/CommonAttrs
         c/AclAttr
         {(s/optional-key :state)              job-states
          :targetResource                      c/NonBlankString
          (s/optional-key :affectedResources)  c/NonEmptyStrList
          :action                              c/NonBlankString
          (s/optional-key :returnCode)         s/Int
          (s/optional-key :progress)           c/NonNegInt
          (s/optional-key :statusMessage)      c/NonBlankString
          (s/optional-key :timeOfStatusChange) c/Timestamp
          (s/optional-key :parentJob)          c/NonBlankString
          (s/optional-key :nestedJobs)         c/NonEmptyStrList}))

;;
;; special functions to facilitate programmatic management of jobs by other resources
;;

(defn set-timestamp
  "Copies the updated timestamp to the timeOfStatusChange field."
  [{:keys [updated] :as entry}]
  (assoc entry :timeOfStatusChange updated))

(defn create
  "Creates a new job and adds it to the database.  Unlike the add function
   this returns just the job URI (or nil if there is an error).  This is
   useful when creating jobs in the process of manipulating other resources."
  [resource]
  (-> resource
      (assoc :id (uuid->uri (u/random-uuid)))
      (u/strip-service-attrs)
      (merge {:resourceURI resource-uri
              :state       "QUEUED"
              :progress    0})
      (u/update-timestamps)
      (set-timestamp)
      (c/validate)
      (db/add)))

(defn value-as-string
  "Converts non-collection values to a string. "
  [v]
  (if (coll? v)
    v
    (if (or (keyword? v) (symbol? v))
      (name v)
      (str v))))

(defn properties-map
  "Converts the given map into a properties map, with all keys
   and values converted into strings.  If the map is nil or
   empty, then an empty map is returned."
  [props]
  (if (seq props)
    {:properties (w/prewalk value-as-string props)}
    {}))

(defn launch
  "Creates a new job, returning an 'accepted' ring response with
   the CIMI-Job-URI header set.  If the optional props arguments is
   given then the values will be used for job properties.  All of the
   keys and values in the properties will by transformed to strings."
  [uri action & [props]]
  (let [job-map (merge
                  {:acl job-acl :targetResource uri :action action}
                  (properties-map props))]
    (if-let [job-resp (crud/add job-map)]                   ;; FIXME: job-map needs to be ring request!
      (let [job-uri (get-in job-resp [:headers "Location"])]
        (-> (r/response nil)
            (r/status 202)
            (r/header "CIMI-Job-URI" job-uri)))
      (-> (str "cannot create job [" action ", " uri "]")
          (r/response)
          (r/status 500)))))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Job))
(defmethod c/validate resource-uri
           [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-name
           [resource resource-name]
  (a/add-acl resource (friend/current-authentication)))

;; specialized to allow the "stop" action
(defmethod c/set-operations resource-uri
           [resource request]
  (try
    (a/modifiable? resource request)
    (let [href (:id resource)
          ops [{:rel (:edit c/action-uri) :href href}
               {:rel (:delete c/action-uri) :href href}
               {:rel (:stop c/action-uri) :href (str href "/stop")}]]
      (assoc resource :operations ops))
    (catch Exception e
      (dissoc resource :operations))))

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

(def query-impl (crud/get-query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))

;; FIXME: Provide real implementation!
(defn do-stop
  [{:keys [id] :as job}]
  (log/info "STOP action for job" id))

(defn do-action
  [{{:keys [uuid action]} :params :as request}]
  (let [job (->> (str resource-name "/" uuid)
                 (db/retrieve)
                 (a/modifiable? request))]
    (cond
      (= action "stop") (do-stop job)
      :else (throw (u/ex-client request (str "unknown action: " action))))))

