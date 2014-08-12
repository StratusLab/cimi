;
; Copyright 2013 Centre National de la Recherche Scientifique (CNRS)
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
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq])
  (:import
    [com.couchbase.client.protocol.views DesignDocument ViewDesign]))

(def ^:const design-doc-name "cimi.0")

(def view-map
  {:user-ids ;; view on user identifiers, user may have more than one
    "
    function(doc, meta) {
      if (meta.type==\"json\") {
        if (doc.username) {
          emit(doc.username, null);
        }
        if (doc.altnames) {
          for (var k in doc.altnames) {
            emit(doc.altnames[k], null);
          }
        }
      }
    }"

   :resource-uri ;; view on resource type (full CIMI URI)
    "
    function(doc, meta) {
      if (meta.type==\"json\" && doc.resourceURI) {
        emit(doc.resourceURI,null);
      }
    }
    "

   :resource-type ;; view on resource type and VIEW right principal
    "
    function (doc, meta) {
      if (meta.type==\"json\") {
        var i=meta.id.indexOf('/');
        if (i>0) {
          var rt=meta.id.substring(0, i);
          emit([rt, \"ROLE_::ADMIN\"], null);

          if (doc.acl) {
            if (doc.acl.owner) {
              var owner = doc.acl.owner;
              if (owner.principal && owner.type) {
                if (owner.type != \"ROLE\" && owner.principal != \"::ADMIN\") {
                  emit([rt, owner.type + \"_\" + owner.principal], null);
                }
              }
            }

            if (doc.acl.rules) {
              for (i=0; i<doc.acl.rules.length; i++) {
                var rule = doc.acl.rules[i];

               if (rule.principal && rule.type) {
                 emit([rt, rule.type + \"_\" + rule.principal], null);
               }
              }
            }
          }
        }
      }
    }
    "
   })

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
    (try
      (.getDesignDocument java-cb-client design-doc-name)
      false
      (catch Exception e
        (log/warn "could not retrieve design document" design-doc-name "->" (str e))
        (->> (create-design-doc)
             (.createDesignDoc java-cb-client))))))

(defn get-view
  "Returns the Couchbase view associated with the given keyword."
  [cb-client view-kw]
  (cbc/get-view cb-client design-doc-name (name view-kw)))

(defn check-view
  "Performs a dummy query against the given view.  Returns true if
   the query succeeded or nil otherwise."
  [cb-client view-kw]
  (let [opts {:include-docs false
              :key          "dummy-key"
              :limit        1
              :stale        false
              :on-error     :continue}
        q (cbq/create-query opts)
        v (get-view cb-client view-kw)]
    (try
      (cbc/query cb-client v q)
      true
      (catch Exception e
        (log/warn "error accessing view:" (name view-kw) (str e))
        nil))))

(defn retry-check-view
  "Execute the check of the given view a maximum of n times with a
   delay of t milliseconds between attempts.  Rethrows the caught
   exception if the check fails after n attempts."
  [cb-client view-kw n t]
  (loop [n n]
    (if-not (check-view cb-client view-kw)
      (if (zero? n)
        (log/error "fatal error accessing view:" (name view-kw))
        (do
          (Thread/sleep t)
          (recur (dec n)))))))

(defn views-available?
  "Checks that all defined views are available."
  [cb-client]
  (doall (map #(retry-check-view cb-client % 3 1000) (keys view-map))))


