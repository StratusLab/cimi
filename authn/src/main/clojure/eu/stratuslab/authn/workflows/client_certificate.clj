(ns eu.stratuslab.authn.workflows.client-certificate
  (:require
    [clojure.tools.logging :as log]
    [clojure.pprint :refer [pprint]]
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.util :as futil])
  (:import
    [eu.emi.security.authn.x509.proxy ProxyUtils]
    [javax.servlet.http HttpServletRequest]
    [org.italiangrid.voms VOMSAttribute VOMSValidators]))

(defn process-voms-attr [^VOMSAttribute result]
  (log/info "VOMS PRIMARY FQAN" (.getPrimaryFQAN result))
  (log/info "VOMS VO" (.getVO result))
  (log/info "VOMS FQANs" (.getFQANs result))
  (log/info "VOMS GAs" (.getGenericAttributes result)))

(defn debug-certificates
  [^HttpServletRequest request]
  (if request
    (try
      (let [chain (.getAttribute request "javax.servlet.request.X509Certificate")]
        (if (ProxyUtils/isProxy (first chain))
          (do
            (log/info "TREATING PROXY CERTIFICATE")
            (log/info "PROXY DN ORIGINAL DN" (ProxyUtils/getOriginalUserDN chain))
            (let [validator (VOMSValidators/newValidator)
                  voms-attrs (.validate validator chain)]
              (doall (map process-voms-attr voms-attrs))))))
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
                                                {:ssl-client-cert       ssl-client-cert
                                                 :ssl-client-cert-chain cert-chain}
                                                {::friend/workflow :client-certificate}))]
            (workflows/make-auth user-record {::friend/workflow          :client-certificate
                                              ::friend/redirect-on-auth? false})))))))

(defn extract-subject-dn
  "Given a X509 certifcate chain, this will extract the DN of the subject
   in the standard RFC2253 format.  If no certificates are provided or
   an exception occurs, then nil is returned."
  [chain]
  (try
    (if-let [cert (first chain)]
      (if (ProxyUtils/isProxy cert)
        (.. chain
            (ProxyUtils/getOriginalUserDN)
            (getName))
        (.. cert
            (getSubjectX500Principal)
            (getName))))
    (catch Exception e
      (log/warn "error while processing client certificate:" (str e)))))

