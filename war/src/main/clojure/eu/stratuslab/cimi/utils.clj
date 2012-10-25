(ns eu.stratuslab.cimi.utils
  "General utilities for use in the CIMI implementation."
  (:import [java.util UUID]))

(defn create-uuid
  "Creates a new, random UUID as a string."
  []
  (str (UUID/randomUUID)))
