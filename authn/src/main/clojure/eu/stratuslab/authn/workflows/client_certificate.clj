(ns eu.stratuslab.authn.workflows.client-certificate
  (:require
    [cemerick.friend :as friend]
    [cemerick.friend.workflows :as workflows]
    [cemerick.friend.util :as futil]))

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
          (workflows/make-auth user-record {::friend/workflow :client-certificate}))))))

(defn extract-subject-dn
  "Given a chain of X509 certificates, this method extracts
   the subject DN as a String from the first of those certificates."
  [chain]
  (if-let [cert (first chain)]
    (.. cert
        (getSubjectX500Principal)
        (getName))))