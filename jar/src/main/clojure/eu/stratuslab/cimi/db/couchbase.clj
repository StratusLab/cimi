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
      (-> (r/response (str "created " {:uri id :request-method :post}))
          (r/status 201)
          (r/header "Location" id))
      (throw (u/ex-conflict {:uri id :request-method :post}))))

  (retrieve
    [this id]
    (or
      (cbc/get-json cb-client id)
      (throw (u/ex-not-found {:uri id :request-method :get}))))

  (edit
    [this {:keys [id] :as resource}]
    (if (cbc/set-json cb-client id resource)
      (r/response resource)
      (throw (u/ex-conflict {:uri id :request-method :put}))))

  (delete
    [this {:keys [id] :as resource}]
    (if (cbc/delete cb-client id)
      (-> (r/response (str id " deleted"))
          (r/status 204))
      (throw (u/ex-not-found {:uri id :request-method :delete}))))

  (query
    [this collection-id {:keys [principals] :as options}]
    (u/viewable-resources cb-client collection-id principals {}))

  )

(defn create
  [cb-client]
  (CouchbaseOps. cb-client))

