(ns eu.stratuslab.authn.vm-rest.utils
  (:import [java.util UUID]))

(defn create-uuid
  []
  (str (UUID/randomUUID)))
