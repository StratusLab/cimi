(ns eu.stratuslab.cimi.resources.utils
  "General utilities for dealing with resources."
  (:require [clojure.tools.logging :refer [debug info warn error]])
  (:import [java.util UUID Date]))

(defn create-uuid
  "Provides a randomized UUID as a string."
  []
  (str (UUID/randomUUID)))

(defn set-time-attributes
  "Sets the updated and created attributes in the request.  If the
  existing? is nil/false, then the created attribute it set;
  otherwise, it is removed from the request."
  ([data]
    (let [now (Date.)
          created (or (:created data) now)]
      (assoc data :created created :updated now)))
  ;; TODO: Remove this arity.
  ([existing? data]
  (let [now (Date.)]
    (if existing?
      (dissoc (assoc data :updated now) :created)
      (assoc data :updated now :created now)))))
