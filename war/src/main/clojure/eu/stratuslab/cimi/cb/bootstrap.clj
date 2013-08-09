(ns eu.stratuslab.cimi.cb.bootstrap
  "Provides the utility to provide the necessary views and objects
   in the Couchbase database for minimal operation of the CIMI 
   service."
  (:require 
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep])
  (:import
    [com.couchbase.client CouchbaseClient]
    [com.couchbase.client.protocol.views DesignDocument ViewDesign]))

(def ^:const design-doc-name "cimi.0")

(def ^:const doc-id-view "
function(doc, meta) {
  emit(meta.id, null);
}")

(def ^:const resource-uri-view "
function(doc, meta) {
  if (meta.type==\"json\" && doc.resourceURI) {
    emit(doc.resourceURI,null);
  }
}")

(defn create-design-doc [cb-client]
  (let [views [(ViewDesign. "doc-id" doc-id-view)
               (ViewDesign. "resource-uri" resource-uri-view)]]
    (DesignDocument. design-doc-name views nil)))

(defn create-views
  "Ensure that the views necessary for searching the database
   are available."
  [cb-client]
  (let [design-doc (create-design-doc cb-client)
        ;; FIXME: This is NOT working with the synchronous method!
        added? (.asyncCreateDesignDoc (cbc/get-client cb-client) design-doc)]
    (if added?
      (log/info "design document " design-doc-name " added to database")
      (log/info "design document " design-doc-name " NOT added to database"))))

(defn create-cep
  "Checks to see if the CloudEntryPoint exists in the database;
   if not, it will create one.  The CloudEntryPoint is the core
   resource of the service and must exist."
  [cb-client]
  (try 
    (cep/create cb-client)
    (log/info "created CloudEntryPoint")
    (catch Exception e
      (log/warn "could not create CloudEntryPoint: " (.getMessage e))
      (try
        (cep/retrieve cb-client)
        (log/info "CloudEntryPoint exists")
        (catch Exception e
          (log/error "problem retrieving CloudEntryPoint: " (.getMessage e)))))))

(defn bootstrap [cb-client]
  (create-cep cb-client)
  (create-views cb-client))

