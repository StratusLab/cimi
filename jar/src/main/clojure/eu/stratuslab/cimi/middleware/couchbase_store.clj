(ns eu.stratuslab.cimi.middleware.couchbase-store
  "Session store using Couchbase for storage.  The sessions will
   only live for the TTL (in seconds) given.  Reading or writing
   the session will update the expiration time."
  (:require [couchbase-clj.client :as cbc]
            [ring.middleware.session.store :refer [SessionStore]])
  (:import (java.util UUID)))

(def ^:const default-ttl (* 15 60)) ; 15 minute TTL in seconds

(defn key->docid [key]
  (str "Session/" key))

(deftype CouchbaseStore [cb-client ttl]
  SessionStore
  (read-session [_ key]
    (if-let [cas-value (cbc/get-touch cb-client (key->docid key) {:expiry ttl})]
      (cbc/cas-val cas-value)))
  (write-session [_ key data]
    (let [key (or key (str (UUID/randomUUID)))]
      (cbc/set cb-client (key->docid key) data {:expiry ttl})
      key))
  (delete-session [_ key]
    (cbc/delete cb-client (key->docid key))
    nil))

(defn couchbase-store
  "Creates session storage engine using Couchbase for storage.  The TTL 
   (in seconds) values follow the rules for Couchbase: -1 no expiration,
   <30 days relative TTL, >30 days absolute TTL.  The default value is
   15 minutes."
  ([cb-client] (couchbase-store cb-client default-ttl))
  ([cb-client ttl] (CouchbaseStore. cb-client ttl)))