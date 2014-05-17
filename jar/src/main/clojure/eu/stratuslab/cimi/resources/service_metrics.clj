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
(ns eu.stratuslab.cimi.resources.service-metrics
  "Provides metrics concerning the performance of the service such as
   the rate of requests, number of responses by error type, and request
   timing."
  (:require
    [eu.stratuslab.cimi.resources.impl.schema :as schema]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.cb.views :as views]
    [compojure.core :refer [defroutes let-routes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [cemerick.friend :as friend]
    [clojure.tools.logging :as log]
    [metrics.utils :as mu]
    [metrics.ring.expose :as me]))

(def ^:const resource-type "ServiceMetrics")

(def ^:const type-uri (str "http://stratuslab.eu/cimi/1/" resource-type))

(def ^:const base-uri (str "/cimi/" resource-type))

(def resource-acl {:owner {:principal "::ADMIN" :type "ROLE"}
                   :rules [{:principal "::ANON" :type "ROLE" :right "VIEW"}]})

(defn add-rops
  "Adds the resource operations to the given resource."
  [resource]
  (if (a/can-modify? (:acl resource))
    (let [href (:id resource)
          ops [{:rel (:edit schema/action-uri) :href href}
               {:rel (:delete schema/action-uri) :href href}]]
      (assoc resource :operations ops))
    resource))

(defn add-acl [resource]
  (assoc resource :acl {:owner {:principal "::ADMIN"
                                :type      "ROLE"}
                        :rules [{:principal "::ANON"
                                 :type      "ROLE"
                                 :right     "VIEW"}]}))

(defn get-metrics
  []
  (into {} (map me/render-to-basic (mu/all-metrics))))

(defn retrieve
  "Returns the data associated with the requested ServiceMessage
   entry (identified by the uuid)."
  []
  (if-let [json (get-metrics)]
    (if (a/can-view? (friend/current-authentication) (:acl json))
      (r/response (add-rops json))
      (u/unauthorized))
    (r/not-found nil)))

(defroutes resource-routes
           (GET base-uri []
                (if (a/can-view? resource-acl)
                  (retrieve)
                  (u/unauthorized)))
           (ANY base-uri []
                (u/bad-method)))

(defroutes routes
           resource-routes)
