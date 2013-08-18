(ns eu.stratuslab.cimi.cb.views
  "Provides constants and utilities for the views used to
   index and search the CIMI documents in the Couchbase
   database.

   NOTE: The view methods may not work when using a local network
   configured with 10.0.x.x addresses.  The symptom is that the 
   connection to the database will timeout.  Information about this
   problem can be found here:

   http://www.couchbase.com/issues/browse/JCBC-151

   The workarounds in the given ticket may or may not work to 
   resolve the problem."

  (:require 
    [couchbase-clj.client :as cbc])
  (:import
    [com.couchbase.client.protocol.views DesignDocument ViewDesign]))

(def ^:const design-doc-name "cimi.0")

(def view-map
  {:doc-id ;; view on document IDs (relative URLs of resources)
   "
function(doc, meta) {
  emit(meta.id, null);
}"
  
  :resource-uri ;; view on resource type (full CIMI URI)
  "
function(doc, meta) {
  if (meta.type==\"json\" && doc.resourceURI) {
    emit(doc.resourceURI,null);
  }
}"})

(defn create-design-doc
  "Creates the Couchbase design document that includes all of the 
   views needed to query the database."
  []
  (let [views (map (fn [[k v]] (ViewDesign. (name k) v)) view-map)]
    (DesignDocument. design-doc-name views nil)))

(defn add-design-doc
  "Add the design document to the database.  Returns true if the 
   document was created; false otherwise."
  [cb-client]
  (let [java-cb-client (cbc/get-client cb-client)]
    (->> (create-design-doc)
      (.createDesignDoc java-cb-client))))

(defn get-view
  "Returns the Couchbase view associated with the given keyword."
  [cb-client view-kw]
  (cbc/get-view cb-client design-doc-name (name view-kw)))

