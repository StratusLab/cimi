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

(ns eu.stratuslab.cimi.db.memory
  "In memory 'database' for tests."
  (:require
    [fs.core :as fs]
    [clojure.data.json :as json]
    [clojure.pprint :refer [pprint]]
    [ring.util.response :as r]
    [eu.stratuslab.cimi.db.protocol :refer [Operations]]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [clojure.string :as str]))

(def dbref (ref nil))

(defn id->path
  [id]
  (->> (str/split id #"/")
       (remove nil?)))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure.  (From clojure.contrib.)"
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(deftype MemoryOps [db-prefix]
  Operations

  (close
    [this]
    (dosync
      (ref-set dbref nil)))

  (add
    [this {:keys [id] :as resource}]
    (let [path (id->path id)]
      (dosync
        (if (get-in @dbref path)
          (let [msg (str id " already exists")]
            (throw (u/ex-conflict {:uri id :request-method :post})))
          (do
            (alter dbref update-in path (constantly resource))
            (-> (r/response (str "created " id))
                (r/status 201)
                (r/header "Location" id)))))))

  (retrieve
    [this id]
    (let [path (id->path id)]
      (or (get-in @dbref path)
          (let [msg (str id " doesn't exist")]
            (throw (u/ex-not-found {:uri id :request-method :get}))))))

  (edit
    [this {:keys [id] :as resource}]
    (let [path (id->path id)]
      (dosync
        (if (get-in @dbref path)
          (do
            (alter dbref update-in path (constantly resource))
            resource)
          (let [msg (str id " doesn't exist")]
            (throw (u/ex-not-found {:uri id :request-method :put})))))))

  (delete
    [this {:keys [id] :as resource}]
    (let [path (id->path id)]
      (dosync
        (if (get-in @dbref path)
          (do
            (alter dbref dissoc-in path)
            (-> (str id " deleted")
                (r/response)
                (r/status 204)))
          (let [msg (str id " doesn't exist")]
            (throw (u/ex-not-found {:uri id :request-method :delete})))))))

  (query
    [this collection-id options]
    (let [path (id->path collection-id)]
      (if (= 1 (count path))                                ;; heuristic for collection ids
        (or
          (vals (get-in @dbref path))
          [])
        (let [msg (str collection-id " isn't a collection")]
          (throw (u/ex-client {:uri collection-id :request-method :get}))))))

  )
