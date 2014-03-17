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

(ns eu.stratuslab.cimi.resources.service-configuration
  "Management of the configuration of the cloud services."
  (:require
    [clojure.string :as str]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [clojure.data.json :as json]
    [eu.stratuslab.cimi.resources.schema :as schema]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.cimi.resources.auth-utils :as a]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [cemerick.friend :as friend]
    [clojure.tools.logging :as log]))

(def ^:const resource-type "ServiceConfiguration")

(def ^:const collection-resource-type "ServiceConfigurationCollection")

(def ^:const type-uri (str "http://stratuslab.eu/cimi/1/" resource-type))

(def ^:const collection-type-uri (str "http://stratuslab.eu/cimi/1/" collection-resource-type))

(def ^:const base-uri (str "/" resource-type))

(def collection-acl {:owner {:principal "::ADMIN" :type "ROLE"}})

(def validate (u/create-validation-fn schema/ServiceConfiguration))

(defn uuid->uri
  "Convert uuid to a ServiceConfiguration resource.  The UUID
   is the concatenation of the :service and :instance values.
   If the :instance isn't set (a general configuration file), then
   the UUID is just the value of the :service key."
  [uuid]
  (str resource-type "/" uuid))

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
  (if (a/can-modify? (:acl resource))
    (let [href (:id resource)
          ops [{:rel (:edit schema/action-uri) :href href}
               {:rel (:delete schema/action-uri) :href href}]]
      (assoc resource :operations ops))
    resource))

(defn add-acl
  "ACL allowing only the administrators to view and modify
  the service configurations."
  [m]
  (let [acl {:owner {:principal "::ADMIN" :type "ROLE"}}]
    (assoc m :acl acl)))

(defn add
  "Adds a new ServiceConfiguration to the database."
  ([cb-client] (add cb-client {}))

  ([cb-client entry]
   (let [uuid (->> entry
                   ((juxt :service :instance))
                   (remove nil?)
                   (str/join "."))
         uri (uuid->uri uuid)
         entry (-> entry
                   (u/strip-service-attrs)
                   (assoc :id uri)
                   (assoc :resourceURI type-uri)
                   (u/set-time-attributes)
                   (add-acl)
                   (validate))]
     (if (cbc/add-json cb-client uri entry)
       (r/created uri)
       (r/status (r/response (str "cannot create " uri)) 400)))))

(defn retrieve
  "Returns the ServiceConfiguration associated with this UUID."
  [cb-client uuid]
  (if-let [json (cbc/get-json cb-client (uuid->uri uuid))]
    (if (a/can-view? (friend/current-authentication) (:acl json))
      (r/response (add-rops json))
      (u/unauthorized))
    (r/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Updates the given resource with the new information.  This will 
   validate the new entry before updating it."
  [cb-client uuid entry]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (friend/current-authentication) (:acl current))
        (let [updated (->> entry
                           (u/strip-service-attrs)
                           (merge current)
                           (u/set-time-attributes)
                           (add-rops)
                           (validate))]
          (if (cbc/set-json cb-client uri updated)
            (r/response updated)
            (r/status (r/response nil) 409))) ;; conflict
        (u/unauthorized))
      (r/not-found nil))))

(defn delete
  "Deletes the named ServiceConfiguration."
  [cb-client uuid]
  (let [uri (uuid->uri uuid)]
    (if-let [current (cbc/get-json cb-client uri)]
      (if (a/can-modify? (friend/current-authentication) (:acl current))
        (if (cbc/delete cb-client uri)
          (r/response nil)
          (r/not-found nil))
        (u/unauthorized))
      (r/not-found nil))))

(defn query
  "Searches the database for resources of this type, taking into
   account the given options."
  [cb-client & [opts]]
  (let [principals (a/authn->principals (friend/current-authentication))
        configs (u/viewable-resources cb-client resource-type principals opts)
        configs (map add-rops configs)
        collection (add-cops {:resourceURI collection-type-uri
                              :id          base-uri
                              :count       (count configs)})]
    (r/response (if (empty? collection)
                  collection
                  (assoc collection :serviceConfigurations configs)))))

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
