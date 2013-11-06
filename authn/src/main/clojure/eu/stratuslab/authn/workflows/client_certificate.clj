(ns eu.stratuslab.authn.workflows.client-certificate
  (:require
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [pprint]]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.util :as futil])
  (:import
    eu.emi.security.authn.x509.proxy.ProxyUtils
    (javax.servlet.http HttpServletRequest)))

(defn debug-certificates
  [^HttpServletRequest request]
  (if request
    (do
      (log/info "DEBUG DEBUG DEBUG" (with-out-str (pprint request)))
      (log/info "DEBUG CERTIFICATE(S):" (.getAttribute request "javax.servlet.request.X509Certificate"))
      (log/info "DEBUG PEER CERTIFICATE(S):" (.getAttribute request "javax.net.ssl.peer_certificates"))
      (log/info "DEBUG DEBUG DEBUG NAMES" (enumeration-seq (.getAttributeNames request))))
    (log/info "SERVLET REQUEST IS NIL")))

(defn client-certificate
  "Friend workflow that extracts a SSL client certificate and passed that to
   the credential function.  The credential function must be defined in the 
   configuration of this workflow or in the overall friend configuration.  The
   credential function will receive a map with a single key :ssl-client-cert, 
   which is a list of X509Certificates.  The ::friend/workflow key in the returned
   authentication map is set to :client-certificate."
  [& {:keys [credential-fn] :as form-config}]
  (fn [{:keys [ssl-client-cert servlet-request] :as request}]
    (debug-certificates servlet-request)
    (when ssl-client-cert
      (if-let [credential-fn (futil/gets :credential-fn form-config (::friend/auth-config request))]
        (if-let [user-record (credential-fn (with-meta
                                              {:ssl-client-cert ssl-client-cert}
                                              {::friend/workflow :client-certificate}))]
          (workflows/make-auth user-record {::friend/workflow          :client-certificate
                                            ::friend/redirect-on-auth? false}))))))

(defn extract-subject-dn
  "Given a X509 certifcate, this will extract the DN of the subject
   in the standard RFC2253 format.  Any exceptions will be caught and
   logged; nil will be returned in this case."
  [x509 servlet-request]
  (try
    (.. x509
        (getSubjectX500Principal)
        (getName))
    (catch Exception e
      (log/error "invalid certificate:" (str e))
      nil)))

