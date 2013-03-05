(ns eu.stratuslab.cimi.resources.utils
  "General utilities for dealing with resources."
  (:require
    [clj-time.core :as time]
    [clj-time.format :as time-fmt])
  (:import [java.util UUID Date]))

(defn create-uuid
  "Provides a randomized UUID as a string."
  []
  (str (UUID/randomUUID)))

(defn set-time-attributes
  "Sets the updated and created attributes in the request.  If the
  existing? is nil/false, then the created attribute it set;
  otherwise, it is removed from the request."
  [data]
  (let [now (time-fmt/unparse (:date-time time-fmt/formatters) (time/now))
        created (or (:created data) now)]
    (assoc data :created created :updated now)))
