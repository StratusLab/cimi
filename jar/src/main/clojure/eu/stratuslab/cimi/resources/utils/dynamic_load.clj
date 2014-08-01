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

(ns eu.stratuslab.cimi.resources.utils.dynamic-load
  "Utilities for loading information from CIMI resources dynamically."
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [clojure.java.classpath :as cp]
    [clojure.tools.namespace.find :as nsf]))

(defn cimi-resource?
  "If the given symbol represents a resource namespace, the symbol
   is returned; nil otherwise.  Resource namespaces have the prefix
   'eu.stratuslab.cimi.resources.'. "
  [sym]
  (->> (name sym)
       (re-matches #"^eu\.stratuslab\.cimi\.resources\.[\w-]+$")
       (first)))

(defn resources
  "Returns the namespaces of all CIMI resources available on the
   classpath."
  []
  (->> (cp/classpath)
       (nsf/find-namespaces)
       (filter cimi-resource?)))

(defn load-resource
  "Dynamically loads the given namespace, returning the namespace.
   Will return nil if the namespace could not be loaded."
  [resource-ns]
  (try
    (require resource-ns)
    (log/info "loaded resource namespace:" (name resource-ns))
    resource-ns
    (catch Exception e
      (log/warn "could not load resource namespace:" (name resource-ns)))))

(defn get-ns-var
  "Retrieves the named var in the given namespace, returning
   nil if the var could not be found.  Function logs the success or
   failure of the request."
  [varname resource-ns]
  (if-let [value (-> resource-ns
                     (name)
                     (str "/" varname)
                     (symbol)
                     (find-var))]
    (do
      (log/info "retrieved" varname "for" (name resource-ns))
      value)
    (do
      (log/warn "did NOT retrieve" varname "for" (name resource-ns)))))

(defn get-resource-link
  "Returns a vector with the resource tag keyword and map with the
   :href keyword associated with the relative URL for the resource.
   Function returns nil if either value cannot be found for the
   resource."
  [resource-ns]
  (if-let [vtag (get-ns-var "resource-tag" resource-ns)]
    (if-let [vtype (get-ns-var "resource-name" resource-ns)]
      [(deref vtag) {:href (deref vtype)}])))

(defn resource-routes
  "Returns a lazy sequence of all of the routes for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-resource)
       (map (partial get-ns-var "routes"))
       (remove nil?)
       (map deref)))

(defn get-resource-links
  "Returns a lazy sequence of all of the resource links for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-resource)
       (map get-resource-link)
       (remove nil?)))
