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
  "middleware to add the :base-uri key and value to the request")

(defn get-host-port
  "Get the host:port value for the request, preferring the 'host'
   header value over local server name and port."
  [{:keys [headers server-name server-port]}]
  (or (get headers "host")
      (format "%s:%d" server-name server-port)))

(defn get-scheme
  "Get the scheme to use for the base URI, preferring the header
   set by the proxy for the remote scheme being used (usually https)."
  [{:keys [headers scheme]}]
  (or (get headers "x-forwarded-proto")
      (name scheme)))

(defn construct-base-uri
  [req]
  (format "%s://%s/cimi/" (get-scheme req) (get-host-port req)))

(defn wrap-base-uri
  "adds the :base-uri key to the request with the base URI value"
  [handler]
  (fn [req]
    (let [base-uri (construct-base-uri req)]
      (-> req
          (assoc :base-uri base-uri)
          (handler)))))
