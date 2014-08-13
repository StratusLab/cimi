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

(ns eu.stratuslab.cimi.db.couchbase
  "Uses a file system as the 'database' for the application."
  (:require
    [ring.util.response :as r]
    [eu.stratuslab.cimi.db.protocol :refer [Operations]]
    [eu.stratuslab.cimi.db.cb.bootstrap :as cbinit]
    [eu.stratuslab.cimi.resources.utils.utils :as u]
    [couchbase-clj.client :as cbc]
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.utils.auth-utils :as a]))

(deftype CouchbaseOps [cb-client]
  Operations

  (bootstrap
    [this]
    (cbinit/bootstrap cb-client))

  (close
    [this]
    (log/info "shutting down Couchbase client")
    (try
      (cbc/shutdown cb-client 3000)
      (catch Exception e
        (log/error "error shutting down Couchbase client: " (str e)))))

  (add
    [this {:keys [id] :as resource}]
    (if (cbc/add-json cb-client id resource)
      (-> (r/response (str "created " id))
          (r/status 201)
          (r/header "Location" id))
      (let [msg (str id " already exists")]
        (throw (u/ex-response 409 msg id)))))

  (retrieve
    [this id]
    (or
      (cbc/get-json cb-client id)
      (let [msg (str id " doesn't exist")]
        (throw (u/ex-response 404 msg id)))))

  (edit
    [this {:keys [id] :as resource}]
    (if (cbc/set-json cb-client id resource)
      (r/response resource)
      (throw (u/ex-response 409 (str "error updating " id) nil))))

  (delete
    [this {:keys [id] :as resource}]
    (if (cbc/delete cb-client id)
      (-> (r/response (str id " deleted"))
          (r/status 204))
      (throw (u/ex-response 404 (str id " doesn't exist") id))))

  (query
    [this collection-id options]
    (let [principals (a/authn->principals)]
      (u/viewable-resources cb-client collection-id principals options)))

  )

(defn create
  [cb-client]
  (CouchbaseOps. cb-client))

