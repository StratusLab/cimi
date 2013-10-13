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
(ns eu.stratuslab.cimi.resources.service-message
  "Allows administrators to provide general service messages for users.
   A short title is provided in the 'name' attribute and the 'description'
   attribute holds the full message.  It is recommended that the message
   be formatted in Markdown format.

   NOTE: Unlike for most other resources, the unique identifier for the
   message is the 'created' timestamp in UTC.  This allows the query to 
   return messages in reversed time order."
  (:require
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [eu.stratuslab.cimi.resources.schema :as schema]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.cimi.resources.auth-utils :as a]
    [eu.stratuslab.cimi.resources.job :as job]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as rresp]
    [clojure.tools.logging :as log]))

(def ^:const resource-type "ServiceMessage")

(def ^:const collection-resource-type "ServiceMessageCollection")

(def ^:const type-uri (str "http://stratuslab.eu/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://stratuslab.eu/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/" resource-type))

(def collection-acl {:owner {:principal "::ADMIN" :type "ROLE"}
                     :rules [{:principal "::ANON" :type "ROLE" :right "VIEW"}]})

(def validate (u/create-validation-fn schema/ServiceMessage))

(defn uuid->uri
  "Convert a uuid into the URI for a ServiceMessage resource.
   The URI must not have a leading slash.  The unique identifer
   for these resources is the 'created' timestamp in UTC."
  [uuid]
  (str resource-type "/" uuid))

(defn add-id
  "Creates the entry's identifier by taking creating the uri from
   the created time and returning the entry with the :id attribute
   added."
  [entry]
  (->> (:created entry)
       (uuid->uri)
       (assoc entry :id)))

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
  "Add a new ServiceMessage to the database."
  [cb-client entry]
  (let [entry (-> entry
                  (u/strip-service-attrs)
                  (assoc :resourceURI type-uri)
                  (u/set-time-attributes)
                  (add-id)
                  (validate))
        uri (:id entry)]
    (if (cbc/add-json cb-client uri entry)
      (rresp/created uri)
      (rresp/status (rresp/response (str "cannot create " uri)) 400))))

(defn retrieve
  "Returns the data associated with the requested ServiceMessage
   entry (identified by the uuid)."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (rresp/response (add-rops json))
    (rresp/not-found nil)))

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
                         (u/set-time-attributes)
                         (validate))]
        (if (cbc/set-json cb-client uri updated)
          (rresp/response updated)
          (rresp/status (rresp/response nil) 409))) ;; conflict
      (rresp/not-found nil))))

(defn delete
  "Deletes the ServiceMessage."
  [cb-client uuid]
  (if (cbc/delete cb-client (uuid->uri uuid))
    (rresp/response nil)
    (rresp/not-found nil)))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [q (cbq/create-query (merge {:include-docs true
                                    :key type-uri
                                    :limit 100
                                    :stale false
                                    :on-error :continue}
                                   opts))
        v (views/get-view cb-client :resource-uri)

        messages (->> (cbc/query cb-client v q)
                      (map cbc/view-doc-json)
                      (map add-rops))
        collection (add-cops {:resourceURI collection-type-uri
                              :id base-uri
                              :count (count messages)})]
    (rresp/response (if (empty? messages)
                      collection
                      (assoc collection :serviceMessages messages)))))

(defroutes collection-routes
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

(def resource-routes
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

(defroutes routes
           collection-routes
           resource-routes)
