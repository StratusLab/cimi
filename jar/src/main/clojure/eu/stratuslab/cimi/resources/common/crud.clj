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

(ns eu.stratuslab.cimi.resources.common.crud
  (:require
    [clojure.tools.logging :as log]
    [ring.util.response :as r]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]
    [eu.stratuslab.cimi.resources.common.schema :as c]
    [eu.stratuslab.cimi.db.dbops :as db]))

(defn resource-name-dispatch
  [request]
  (get-in request [:params :resource-name]))

(defn resource-name-and-action-dispatch
  [request]
  (-> request
      :params
      (juxt :resource-name :action)))

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

(defmulti do-action resource-name-and-action-dispatch)

(defmethod do-action :default
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
  (str resource-name "/" (u/random-uuid)))

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

;;
;; Generic implementations of standard CRUD operations
;; that should be appropriate for most resources.
;;

(defn get-add-fn
  [resource-name collection-acl resource-uri]
  (fn [{:keys [body] :as request}]
    (a/modifiable? {:acl collection-acl} request)
    (let [json (u/body->json body)
          uri (new-identifier resource-name json)]
      (-> json
          (u/strip-service-attrs)
          (assoc :id uri)
          (assoc :resourceURI resource-uri)
          (u/update-timestamps)
          (add-acl resource-name)
          (c/validate)
          (db/add)))))

(defn get-retrieve-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (-> (str resource-name "/" uuid)
        (db/retrieve)
        (a/viewable? request)
        (c/set-operations request)
        (r/response))))

(defn get-edit-fn
  [resource-name]
  (fn [{{uuid :uuid} :params body :body :as request}]
    (let [current (-> (str resource-name "/" uuid)
                      (db/retrieve)
                      (a/modifiable? request))]
      (->> (u/body->json body)
           (u/strip-service-attrs)
           (merge current)
           (u/update-timestamps)
           (c/validate)
           (db/edit)))))

(defn get-delete-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (-> (str resource-name "/" uuid)
        (db/retrieve)
        (a/modifiable? request)
        (db/delete))))

(defn wrap-collection-fn
  [resource-name collection-acl collection-uri collection-key]
  (fn [resources]
    (let [n (count resources)
          skeleton {:acl         collection-acl
                    :resourceURI collection-uri
                    :id          resource-name
                    :count       n}]
      (if (zero? n)
        skeleton
        (assoc skeleton collection-key resources)))))

(defn get-query-fn
  [resource-name collection-acl collection-uri collection-key]
  (let [wrapper (wrap-collection-fn resource-name collection-acl collection-uri collection-key)]
    (fn [{:keys [body] :as request}]
      (a/viewable? {:acl collection-acl} request)
      (let [options (u/body->json body)
            collection (->> (a/authn->principals)
                            (assoc options :principals)
                            (db/query resource-name)
                            (map #(c/set-operations % request))
                            (wrapper))]
        (c/set-operations collection request)))))

