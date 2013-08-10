(ns eu.stratuslab.cimi.cb.bootstrap
  "Provides the utility to provide the necessary views and objects
   in the Couchbase database for minimal operation of the CIMI 
   service.

   NOTE: The view methods may not work when using a local network
   configured with 10.0.x.x addresses.  The symptom is that the 
   connection to the database will timeout.  Information about this
   problem can be found here:

   http://www.couchbase.com/issues/browse/JCBC-151

   The workarounds in the given ticket may or may not work to 
   resolve the problem."

  (:require 
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [clj-http.client :as http]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep])
  (:import
    [java.net URI]
    [java.util.concurrent TimeUnit]
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

(defn create-design-doc []
  (let [views [(ViewDesign. "doc-id" doc-id-view)
               (ViewDesign. "resource-uri" resource-uri-view)]]
    (DesignDocument. design-doc-name views nil)))

(defn create-views
  "Ensure that the views necessary for searching the database
   are available."
  [cb-client]
  (let [design-doc (create-design-doc)
        added? (.createDesignDoc (cbc/get-client cb-client) design-doc)]
    (log/info "design document" design-doc-name "added to database")
    (log/info "design document" design-doc-name "NOT added to database")))

(defn create-cep
  "Checks to see if the CloudEntryPoint exists in the database;
   if not, it will create one.  The CloudEntryPoint is the core
   resource of the service and must exist."
  [cb-client]
  (try 
    (cep/create cb-client)
    (log/info "created CloudEntryPoint")
    (catch Exception e
      (log/warn "could not create CloudEntryPoint:" (.getMessage e))
      (try
        (cep/retrieve cb-client)
        (log/info "CloudEntryPoint exists")
        (catch Exception e
          (log/error "problem retrieving CloudEntryPoint:" (.getMessage e)))))))

(defn bootstrap
  "Bootstraps the Couchbase database by creating the CloudEntryPoint
   and inserting the design document with views, as necessary."
  [cb-client]
  (create-cep cb-client)
  (create-views cb-client))

