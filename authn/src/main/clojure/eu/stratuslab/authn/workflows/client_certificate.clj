(ns eu.stratuslab.authn.workflows.client-certificate
  (:require
    [clojure.tools.logging :as log]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.util :as futil])
  (:import
    eu.emi.security.authn.x509.proxy.ProxyUtils
    (javax.servlet.http HttpServletRequest)))

(defn get-client-certs
  "Returns the SSL client certificate of the request, if one exists."
  [^HttpServletRequest request]
  (if request
    (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn get-peer-certs
  "Returns the SSL client certificate of the request, if one exists."
  [^HttpServletRequest request]
  (if request
    (.getAttribute request "javax.net.ssl.peer_certificates")))

(defn client-certificate
  "Friend workflow that extracts a SSL client certificate and passed that to
   the credential function.  The credential function must be defined in the 
   configuration of this workflow or in the overall friend configuration.  The
   credential function will receive a map with a single key :ssl-client-cert, 
   which is a list of X509Certificates.  The ::friend/workflow key in the returned
   authentication map is set to :client-certificate."
  [& {:keys [credential-fn] :as form-config}]
  (fn [{:keys [ssl-client-cert] :as request}]
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
    (log/info "IS PROXY?" (ProxyUtils/isProxy x509))
    (log/info "CERTS" (get-client-certs servlet-request))
    (log/info "PEER CERTS" (get-peer-certs servlet-request))
    (try
      (log/info "ORIGINAL DN" (ProxyUtils/getOriginalUserDN (list x509)))
      (catch Exception e
        (log/info "GOT EXCEPTION" (str e))))
    (try
      (log/info "ORIGINAL DN 2" (ProxyUtils/getOriginalUserDN (get-client-certs servlet-request)))
      (catch Exception e
        (log/info "GOT EXCEPTION" (str e))))
    (try
      (log/info "ORIGINAL DN 3" (ProxyUtils/getOriginalUserDN (get-peer-certs servlet-request)))
      (catch Exception e
        (log/info "GOT EXCEPTION" (str e))))
    (.. x509
        (getSubjectX500Principal)
        (getName))
    (catch Exception e
      (log/error "invalid certificate:" (str e))
      nil)))

