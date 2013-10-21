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
(ns eu.stratuslab.cimi.resources.cloud-entry-point
  "Root resource for CIMI, providing information about the locations
  of other resources within the server."
  (:require
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.schema :as schema]
    [eu.stratuslab.cimi.resources.utils :as u]
    [eu.stratuslab.cimi.resources.auth-utils :as a]
    [eu.stratuslab.cimi.resources.machine-configuration :as mc]
    [eu.stratuslab.cimi.resources.job :as job]

    [eu.stratuslab.cimi.resources.service-message :as sm]

    [eu.stratuslab.cimi.resources.volume :as volume]
    [eu.stratuslab.cimi.resources.volume-template :as volume-template]
    [eu.stratuslab.cimi.resources.volume-configuration :as volume-configuration]
    [eu.stratuslab.cimi.resources.volume-image :as volume-image]

    [compojure.core :refer [defroutes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]))

(def ^:const resource-type "CloudEntryPoint")

(def ^:const type-uri (str "http://schemas.dmtf.org/cimi/1/" resource-type))

(def ^:const base-uri "/")

(def resource-acl {:owner {:principal "::ADMIN" :type "ROLE"}
                   :rules [{:principal "::ANON" :type "ROLE" :right "VIEW"}]})

;; FIXME: Generate these automatically.
(def resource-links
  {:machineConfigs {:href mc/resource-type}
   :jobs {:href job/resource-type}
   :volumes {:href volume/resource-type}
   :volumeTemplates {:href volume-template/resource-type}
   :volumeConfigs {:href volume-configuration/resource-type}
   :volumeImages {:href volume-image/resource-type}
   :serviceMessages {:href sm/resource-type}})

(def validate (u/create-validation-fn schema/CloudEntryPoint))

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (if (a/can-modify? (:acl resource))
    (let [ops [{:rel (:edit schema/action-uri) :href base-uri}]]
      (assoc resource :operations ops))
    resource))

(defn add
  "Creates a minimal CloudEntryPoint in the database.  Note that
   only the common attributes are saved in the database; links to
   resource types are generated when the service starts.

   NOTE: Unlike other resources, the :id is 'CloudEntryPoint'
   rather than the relative URI for the resource."
  [cb-client]

  (let [record (-> {:acl resource-acl
                    :id resource-type
                    :resourceURI type-uri}
                   (u/set-time-attributes))]
    (cbc/add-json cb-client resource-type record {:observe true
                                                  :persist :master
                                                  :replicate :zero})))

(defn retrieve
  "Returns the data associated with the CloudEntryPoint.  This combines
   the values of the common attributes in the database with the baseURI
   from the web container and the generated resource links."
  [cb-client baseURI]
  (if-let [cep (cbc/get-json cb-client resource-type)]
    (r/response (-> cep
                    (assoc :baseURI baseURI)
                    (merge resource-links)
                    (add-rops)))
    (r/not-found nil)))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn edit
  "Update the cloud entry point attributes.  Note that only the common
  resource attributes can be updated.  The active resource collections
  cannot be changed.  For correct behavior, the cloud entry point must
  have been previously initialized.  Returns nil."
  [cb-client baseURI entry]
  (if-let [current (cbc/get-json cb-client resource-type)]
    (let [db-doc (-> entry
                     (select-keys [:name :description :properties])
                     (merge current)
                     (u/set-time-attributes))
          doc (-> db-doc
                  (assoc :baseURI baseURI)
                  (merge resource-links)
                  (add-rops)
                  (validate))]
      (if (cbc/set-json cb-client resource-type db-doc)
        (r/response doc)
        (r/status (r/response nil) 409)))
    (r/not-found nil)))

(defroutes routes
           (GET base-uri {:keys [cb-client base-uri] :as request}
                (if (a/can-view? resource-acl)
                  (retrieve cb-client base-uri)
                  (u/unauthorized)))
           (PUT base-uri {:keys [cb-client base-uri body] :as request}
                (if (a/can-modify? resource-acl)
                  (let [json (u/body->json body)]
                    (edit cb-client base-uri json))
                  (u/unauthorized)))
           (ANY base-uri request
                (u/bad-method)))
