(ns eu.stratuslab.cimi.cb.bootstrap
  "Provides the utility to provide the necessary views and objects
   in the Couchbase database for minimal operation of the CIMI 
   service."
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
   are available.

   NOTE: This method does NOT work because of problems with the 
   Java Couchbase client on Java 1.7 (and possibly other versions).
   The issue is known to the Couchbase developers and is tracked:

   http://www.couchbase.com/issues/browse/JCBC-151

   Use the create-views-rest function which works around this
   problem until the above problem is fixed."
  [cb-client]
  (let [design-doc (create-design-doc)
        added? (.createDesignDoc (cbc/get-client cb-client) design-doc)]
    (log/info "design document" design-doc-name "added to database")
    (log/info "design document" design-doc-name "NOT added to database")))

(defn create-design-doc-json []
  (.toJson (create-design-doc)))

(defn create-views-rest
  "Works around a problem with the Java API to upload the views. 
   See the documentation string for the create-views function."
  [{:keys [uris bucket username password]}]
  (let [design-doc (create-design-doc-json)
        uri (first uris)
        scheme (.getScheme uri)
        host (.getHost uri)
        path (str "/" bucket "/_design/" design-doc-name)
        url (URI. scheme nil host 8092 path nil nil)
        response (http/put (.toString url)
                   {:basic-auth [username password]
                    :body design-doc
                    :content-type :json
                    :socket-timeout 2000  ;; in milliseconds
                    :conn-timeout 2000    ;; in milliseconds
                    :accept :json})]
    (println response)
    (if (= 201 (:status response))
      (log/info "design document" design-doc-name "added to database")
      (log/info "design document" design-doc-name "NOT added to database"))))

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
   and inserting the design document with views, as necessary.

   NOTE: The second parameter is to allow a workaround for a problem
   with the Couchbase java API.  See the create-views function for
   more information."
  [cb-client cb-cfg]
  (create-cep cb-client)
  (create-views-rest cb-cfg))

