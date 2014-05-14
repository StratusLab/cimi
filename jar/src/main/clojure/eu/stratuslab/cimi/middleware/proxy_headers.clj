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

(ns eu.stratuslab.cimi.middleware.proxy-headers
  (:require [clojure.string :as str]))

(def ^:const proxy-port-header
  "X-Forwarded-Port")

(def ^:const proxy-scheme-header
  "X-Forwarded-Scheme")

(defn wrap-proxy-headers
  "If the proxy headers indicating a proxy port and scheme are set,
   then the standard fields in the ring request are updated with these
   values."
  [handler]
  (fn [req]
    (let [headers (:headers req)
          scheme (or (get headers proxy-scheme-header) (:scheme req))
          port (or (get headers proxy-port-header) (:server-port req))]
      (handler
        (-> req
            (assoc :scheme scheme)
            (assoc :server-port port))))))
