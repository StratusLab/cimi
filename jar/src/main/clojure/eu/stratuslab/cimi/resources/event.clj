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
    [clojure.string :as str]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.impl.schema :as schema]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.cb.views :as views]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [schema.core :as s]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :events)

(def ^:const resource-name "Event")

(def ^:const collection-name "EventCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def ^:const base-uri (str c/service-context resource-name))

(def collection-acl {:owner {:principal "::ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "::USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(defn uuid->uri
  "Convert the uuid into a URI for the Event resource."
  [uuid]
  (str resource-name "/" uuid))

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

(def Event
  (merge c/CommonAttrs
         c/AclAttr
         {:timestamp                s/Inst
          :type                     c/NonBlankString
          (s/optional-key :content) (s/either StateContent AlarmContent ModelContent AccessContent)
          :outcome                  outcome-values
          :severity                 severity-values
          (s/optional-key :contact) c/NonBlankString}))

(def validate (u/create-validation-fn Event))

(defn add-cops
  "Adds the collection operations to the given resource."
  [resource]
  (if (a/can-modify? collection-acl)
    (let [ops [{:rel (:add schema/action-uri) :href base-uri}]]
      (assoc resource :operations ops))
    resource))

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (let [href (:id resource)
        ops [{:rel (:edit schema/action-uri) :href href}
             {:rel (:delete schema/action-uri) :href href}]]
    (assoc resource :operations ops)))

(defn add
  "Add a new Event to the database."
  [cb-client entry]
  (let [uri (uuid->uri (u/random-uuid))
        entry (-> entry
                  (u/strip-service-attrs)
                  (merge {:id          uri
                          :resourceURI resource-uri})
                  (u/update-timestamps)
                  (validate))]
    (if (cbc/add-json cb-client uri entry)
      (r/created uri)
      (r/status (r/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the data associated with the requested Job
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (r/response (add-rops json))
    (r/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (let [updated (->> entry
                         (u/strip-service-attrs)
                         (merge current)
                         (u/update-timestamps)
                         (add-rops)
                         (validate))]
        (if (cbc/set-json cb-client uri updated)
          (r/response updated)
          (r/status (r/response nil) 409)))                 ;; conflict
      (r/not-found nil))))

(defn delete
  "Deletes the named event."
  [cb-client uuid]
  (if (cbc/delete cb-client (uuid->uri uuid))
    (r/response nil)
    (r/not-found nil)))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [q (cbq/create-query (merge {:include-docs true
                                    :key          resource-uri
                                    :limit        100
                                    :stale        false
                                    :on-error     :continue}
                                   opts))
        v (views/get-view cb-client :resource-uri)

        configs (->> (cbc/query cb-client v q)
                     (map cbc/view-doc-json)
                     (map add-rops))
        collection (add-cops {:resourceURI collection-uri
                              :id          base-uri
                              :count       (count configs)})]
    (r/response (if (empty? configs)
                  collection
                  (assoc collection :events configs)))))

#_(defroutes collection-routes
           (POST base-uri {:keys [cb-client body]}
                 (if (a/can-modify? collection-acl)
                   (let [json (u/body->json body)]
                     (add cb-client json))
                   (u/unauthorized)))
           (GET base-uri {:keys [cb-client body]}
                (if (a/can-view? collection-acl)
                  (let [json (u/body->json body)]
                    (query cb-client json))
                  (u/unauthorized)))
           (ANY base-uri []
                (u/bad-method)))

#_(def resource-routes
  (let-routes [uri (str base-uri "/:uuid")]
              (GET uri [uuid :as {cb-client :cb-client}]
                   (retrieve cb-client uuid))
              (PUT uri [uuid :as {cb-client :cb-client body :body}]
                   (let [json (u/body->json body)]
                     (edit cb-client uuid json)))
              (DELETE uri [uuid :as {cb-client :cb-client}]
                      (delete cb-client uuid))
              (ANY uri []
                   (u/bad-method))))

#_(defroutes routes
           collection-routes
           resource-routes)
