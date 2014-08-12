;
; Copyright 2014 SixSq Sàrl
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

(ns eu.stratuslab.cimi.db.filesystem
  "Uses a file system as the 'database' for the application."
  (:require
    [fs.core :as fs]
    [clojure.data.json :as json]
    [clojure.pprint :refer [pprint]]
    [ring.util.response :as r]
    [eu.stratuslab.cimi.db.protocol :refer [Operations]]
    [eu.stratuslab.cimi.resources.utils.utils :as u]))

(def db-prefix "testdb/")

(defn serialize
  [resource]
  (with-out-str
    (json/pprint resource :key-fn name)))

(defn serialize-file
  [f resource]
  (->> f
       (fs/parent)
       (fs/mkdirs))
  (->> resource
       (serialize)
       (spit f))
  true)

(defn deserialize
  [s]
  (json/read-str s :key-fn keyword))

(defn deserialize-file
  [f]
  (->> f
       (slurp)
       (deserialize)))

(deftype FilesystemOps [db-prefix]
  Operations

  (close
    [this]
    ;; no-op
    )

  (add
    [this {:keys [id] :as resource}]
    (let [fname (str db-prefix id)]
      (if-not (fs/exists? fname)
        (do
          (serialize-file fname resource)
          (-> (r/response (str "created " id))
              (r/status 201)
              (r/header "Location" id)))
        (let [msg (str id " already exists")]
          (throw (u/ex-response 409 msg id))))))

  (retrieve
    [this id]
    (let [fname (str db-prefix id)]
      (if (fs/exists? fname)
        (deserialize-file fname)
        (let [msg (str id " doesn't exist")]
          (throw (u/ex-response 404 msg id))))))

  (edit
    [this {:keys [id] :as resource}]
    (let [fname (str db-prefix id)]
      (if (fs/exists? fname)
        (serialize-file fname resource)
        (let [msg (str id " doesn't exist")]
          (throw (u/ex-response 404 msg id))))))

  (delete
    [this {:keys [id] :as resource}]
    (let [fname (str db-prefix id)]
      (if (fs/exists? fname)
        (let [msg (str id " deleted")]
          (fs/delete fname)
          (-> (r/response msg)
              (r/status 204)))
        (let [msg (str id " doesn't exist")]
          (throw (u/ex-response 404 msg id))))))

  (query
    [this collection-id options]
    (let [dname (str db-prefix collection-id)]
      (if (fs/directory? dname)
        (map deserialize-file (map #(str dname "/" %) (fs/list-dir dname)))
        (let [msg (str collection-id " isn't a collection")]
          (throw (u/ex-response 400 msg collection-id))))))

  )
