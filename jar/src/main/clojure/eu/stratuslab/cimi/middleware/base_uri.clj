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
    [clojure.tools.logging :as log]))

(defn construct-base-uri
  [{:keys [scheme server-name server-port]}]
  (format "%s://%s:%d%s/" (name scheme) server-name server-port "/cimi"))

(defn wrap-base-uri
  "adds the :base-uri key to the request with the base URI value"
  [handler]
  (fn [req]
    (let [base-uri (construct-base-uri req)]
      (log/debug (format "base-uri=%s" base-uri))
      (-> req
          (assoc :base-uri base-uri)
          (handler)))))
