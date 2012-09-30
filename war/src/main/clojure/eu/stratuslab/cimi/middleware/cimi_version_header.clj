(ns eu.stratuslab.cimi.middleware.cimi-version-header
  (:require [clojure.string :as str]))

(def ^:const cimi-header "CIMI-Specification-Version")

(def ^{:const true
       :doc "Regular expression for valid versions.  Major and minor
             fields must be supplied; update field is optional."}
     version-regex #"^\s*(\d+)\.(\d+)(?:\.(\d+))?\s*$")

(def ^{:const true
       :doc "Versions of the CIMI standard implemented by the server.
             Put the versions in descending order of preference.
             Complete versions with major, minor, and update fields
             are required."}
     spec-versions [["1" "0" "0"]])

(def ^{:doc "Map between exploded specification versions and the
             string representation."}
     spec-versions-map
     (zipmap spec-versions (map #(str/join "." %) spec-versions)))

(defn version-match?
  "Returns true if the two given versions match.  The versions must be
  seqs of the major, minor, update values of a version.  The versions
  are only compared to the shortest version, so for example, [1 0]
  will match [1 0 1].  The values can be anything that is comparable,
  but the types should be consistent. Note that a nil value will match
  everything."
  [v1 v2]
  (every? true? (map = v1 v2)))

(defn parse-version
  "Parse a version string into its major, minor, and update fields.
  The update field is optional.  Invalid input will return nil."
  [s]
  (let [v (next (re-matches version-regex (or s "")))]
    (if v
      (remove nil? v)
      nil)))

(defn requested-versions
  "Parses the string (value of CIMI Specification Version header) and
  provides a list of the requested specification versions.  This list
  is in the order of preference.  The individual versions are list of
  the major, minor, and update values of the versions.  Invalid
  versions are ignored.  Returns nil on a nil input."
  [s]
  (if s 
    (remove nil? (map parse-version (str/split s #"\s*,\s*")))))

(defn select-spec-version
  "Select the implementation version based on the versions requested
  by the client.  If the input is nil, then this returns the first
  implemented version."
  [header]
  (if header
    (let [req-versions (requested-versions header)
          match (first (for [req-version req-versions
                             spec-versions spec-versions
                             :when (version-match? req-version spec-versions)]
                         spec-versions))]
      (get spec-versions-map match))
    (get spec-versions-map (first spec-versions))))

(defn add-cimi-spec-key
  "Adds the key :cimi-specification-version with the negotiated
  CIMI specification version to the request."
  [req version]
  (assoc req :cimi-specification-version version))

(defn add-cimi-spec-header
  "Adds the CIMI specification header to the response with the value
  of the specification version used to process the request."
  [resp version]
  (assoc-in resp [:headers cimi-header] version))

(defn wrap-cimi-version-header
  "Middleware that processes the CIMI-Specification-Version header and
  adds the matched implementation version to the
  key :cimi-specification-version in the request and adds the
  CIMI-Specification-Version header to the response.  Will return a
  400 error response if the requested specification version is not
  supported by the implementation."
  [handler & [options]]
  (fn [req]
    (let [header (get-in req [:headers cimi-header])]
      (if-let [version (select-spec-version header)]
        (let [req (add-cimi-spec-key req version)
              resp (handler req)]
          (add-cimi-spec-header resp version))
        {:status 400
         :headers {}
         :body "implementation does not support requested CIMI specification version"}))))
