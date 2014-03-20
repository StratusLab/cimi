;
; Copyright 2014 Centre National de la Recherche Scientifique (CNRS)
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

(ns eu.stratuslab.cimi.middleware.base-uri
  "middleware to add the :base-uri key and value to the request"
  (:require
    [clojure.string :as s]
    [clojure.tools.logging :as log]))

(defn format-context
  "Formats the given value as a valid context string, which is
   either the empty string or a path beginning with a slash. Passing
   nil returns the empty string.  Multiple consequative slashes
   are reduced to one."
  [context]
  (let [normalized (-> context
                     (or "")
                     (s/replace #"/+" "/"))
        stripped (second (re-matches #"^/*(.*?)/*$" normalized))]
    (if-not (= "" stripped)
      (str "/" stripped)
      stripped)))

(defn wrap-base-uri
  "adds the :base-uri key to the request with the base URI value
   taking into account the context of the end server (e.g. if being
   proxied by nginx, then the context is the path added by the server)"
  [handler & [context]]
  (let [context (format-context context)]
    (fn [req]
      (let [{:keys [scheme server-name server-port]} req
            base-uri (format "%s://%s:%d%s/" (name scheme) server-name server-port context)
            req (assoc req :base-uri base-uri)]
        (log/debug (format "base-uri=%s" base-uri))
        (handler req)))))
