(ns eu.stratuslab.cimi.resources.utils
  "General utilities for dealing with resources."
  (:require
    [clj-time.core :as time]
    [clj-time.format :as time-fmt])
  (:import [java.util UUID Date]))

(defn create-uuid
  "Provides the string representation of a pseudo-random UUID."
  []
  (str (UUID/randomUUID)))

(defn set-time-attributes
  "Sets the updated and created attributes in the request.  If the
  existing? is nil/false, then the created attribute it set;
  otherwise, it is removed from the request."
  [data]
  (let [updated (time-fmt/unparse (:date-time time-fmt/formatters) (time/now))
        created (or (:created data) updated)]
    (assoc data :created created :updated updated)))
