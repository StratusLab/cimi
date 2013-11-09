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

(defn voms-name-and-roles
  "Returns a vector with two values: the VO name and a list of roles
   (actually the FQANs defined in the attribute)."
  [^VOMSAttribute attr]
  (let [vo-name (.getVO attr)
        roles (list (.getFQANs attr))]
    [vo-name, roles]))

(defn voms-vo-info
  "Returns a map containing key-value pairs where the key is the
   VO name (as a string) and the value is a list of associated roles.
   (The roles are the FQANs in the VOMS proxy."
  [chain]
  (when chain
    (try
      (when (ProxyUtils/isProxy (first chain))
        (let [voms-attrs (.. (VOMSValidators/newValidator)
                             (validate chain))]
          (into {} (map voms-name-and-roles voms-attrs))))
      (catch Exception e
        (log/info "exception when treating voms proxy:" (str e))
        nil))))

(defn extract-client-cert-chain
  "Will pull the full certificate chain out of the HttpServletRequest.  The
   returned value is an X509Certificate array or nil if no value is found."
  [^HttpServletRequest request]
  (when request
    (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn extract-subject-dn
  "Given a X509 certifcate chain, this will extract the DN of the subject
   in the standard RFC2253 format.  If no certificates are provided or
   an exception occurs, then nil is returned."
  [chain]
  (try
    (when-let [cert (first chain)]
      (if (ProxyUtils/isProxy cert)
        (.. chain
            (ProxyUtils/getOriginalUserDN)
            (getName))
        (.. cert
            (getSubjectX500Principal)
            (getName))))
    (catch Exception e
      (log/warn "error while processing client certificate:" (str e)))))

