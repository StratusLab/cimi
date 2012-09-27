(ns eu.stratuslab.cimi.utils
  (:import [java.util UUID]))

(defn create-uuid
  []
  (str (UUID/randomUUID)))
