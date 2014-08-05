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

(ns eu.stratuslab.cimi.resources.impl.common-crud
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [schema.core :as s]
    [ring.util.response :as r]
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.impl.common :as c]
    [cemerick.friend :as friend]))

(defn resource-name-dispatch
  [request]
  (get-in request [:params :resource-name]))

(defmulti add resource-name-dispatch)

(defmethod add :default
           [request]
  (u/bad-method request))


(defmulti query resource-name-dispatch)

(defmethod query :default
           [request]
  (u/bad-method request))


(defmulti retrieve resource-name-dispatch)

(defmethod retrieve :default
           [request]
  (u/bad-method request))


(defmulti edit resource-name-dispatch)

(defmethod edit :default
           [request]
  (u/bad-method request))


(defmulti delete resource-name-dispatch)

(defmethod delete :default
           [request]
  (u/bad-method request))

;;
;; Determine the identifier for a new resource.
;; This is normally a random UUID, but may require
;; specialization, for example using the username for
;; user resources.
;;

(defmulti new-identifier
          (fn [resource-name json]
            resource-name))

(defmethod new-identifier :default
           [resource-name json]
  (u/random-uuid))

;;
;; Determine the ACL to use for a new resource.
;; The default is to leave the :acl key blank.
;;

(defmulti add-acl
          (fn [json resource-name]
            resource-name))

(defmethod add-acl :default
           [json resource-name]
  json)

(defn get-add-fn
  [resource-name collection-acl resource-uri]
  (fn [{:keys [cb-client body] :as request}]
    (if (a/can-modify? collection-acl)
      (let [json (u/body->json body)
            uri (str resource-name "/" (new-identifier resource-name json)) ;; method for finding ID may differ between resources
            entry (-> json
                      (u/strip-service-attrs)
                      (assoc :id uri)
                      (assoc :resourceURI resource-uri)
                      (u/update-timestamps)
                      (add-acl resource-name)
                      #_(c/validate))]
        (if (cbc/add-json cb-client uri entry)
          (r/created uri)
          (r/status (r/response (str "cannot create " uri)) 400)))
      (u/unauthorized request))))

(defn get-retrieve-fn
  [resource-name]
  (fn [{{uuid :uuid} :params cb-client :cb-client :as request}]
    (if-let [json (->> (str resource-name "/" uuid)
                       (cbc/get-json cb-client))]
      (if (a/can-view? (:acl json))
        (r/response (c/set-operations json))
        (u/unauthorized request))
      (r/not-found nil))))

;; FIXME: Implementation should use CAS functions to avoid update conflicts.
(defn get-edit-fn
  [resource-name]
  (fn [{{uuid :uuid} :params cb-client :cb-client body :body :as request}]
    (let [uri (str resource-name "/" uuid)]
      (if-let [current (cbc/get-json cb-client uri)]
        (if (a/can-modify? (:acl current))
          (let [json (u/body->json body)
                updated (->> json
                             (u/strip-service-attrs)
                             (merge current)
                             (u/update-timestamps)
                             (c/validate))]
            (if (cbc/set-json cb-client uri updated)
              (r/response updated)
              (u/conflict request)))
          (u/unauthorized request))
        (r/not-found nil)))))

(defn get-delete-fn
  [resource-name]
  (fn [{{uuid :uuid} :params cb-client :cb-client :as request}]
    (let [uri (str resource-name "/" uuid)]
      (if-let [current (cbc/get-json cb-client uri)]
        (if (a/can-modify? (:acl current))
          (if (cbc/delete cb-client uri)
            (r/response nil)
            (r/not-found nil))
          (u/unauthorized request))
        (r/not-found nil)))))

(defn get-query-fn
  [resource-name collection-acl collection-uri collection-name collection-key]
  (fn [{:keys [cb-client body] :as request}]
    (if (a/can-view? collection-acl)
      (let [opts (u/body->json body)
            principals (a/authn->principals)
            configs (u/viewable-resources cb-client resource-name principals opts)
            configs (map c/set-operations configs)
            collection (c/set-operations {:resourceURI collection-uri
                                          :id          collection-name
                                          :count       (count configs)})]
        (r/response (if (empty? collection)
                      collection
                      (assoc collection collection-key configs))))
      (u/unauthorized request))))
