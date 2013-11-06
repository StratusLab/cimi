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
    (try
      (let [attr-value (.getAttribute request "javax.servlet.request.X509Certificate")
            chain (doall (map identity attr-value))]
        (log/info "DEBUG CERTIFICATES:" attr-value)
        (log/info "DEBUG CERTIFICATE CHAIN:" chain)
        (log/info "PROXY?" (ProxyUtils/isProxy (first attr-value)))
        (log/info "PROXY DN ATTR-VALUE" (ProxyUtils/getOriginalUserDN attr-value))
        (log/info "PROXY DN CHAIN" (ProxyUtils/getOriginalUserDN chain)))
      (catch Exception e
        (log/info "GOT EXCEPTION:" (str e))))
    (log/info "SERVLET REQUEST IS NIL")))

(defn extract-client-cert-chain
  "Will pull the full certificate chain out of the HttpServletRequest.  The
   returned value is an X509Certificate array or nil if no value is found."
  [^HttpServletRequest request]
  (if request
    (.getAttribute request "javax.servlet.request.X509Certificate")))

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
      (let [cert-chain (extract-client-cert-chain servlet-request)]
        (if-let [credential-fn (futil/gets :credential-fn form-config (::friend/auth-config request))]
          (if-let [user-record (credential-fn (with-meta
                                                {:ssl-client-cert ssl-client-cert
                                                 :ssl-client-cert-chain cert-chain}
                                                {::friend/workflow :client-certificate}))]
            (workflows/make-auth user-record {::friend/workflow          :client-certificate
                                              ::friend/redirect-on-auth? false})))))))

(defn extract-subject-dn
  "Given a X509 certifcate, this will extract the DN of the subject
   in the standard RFC2253 format.  Any exceptions will be caught and
   logged; nil will be returned in this case."
  [x509]
  (try
    (.. x509
        (getSubjectX500Principal)
        (getName))
    (catch Exception e
      (log/error "invalid certificate:" (str e))
      nil)))

