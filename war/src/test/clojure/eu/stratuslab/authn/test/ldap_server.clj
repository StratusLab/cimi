(ns eu.stratuslab.authn.test.ldap-server
  "An embedded ldap server for unit testing.  This file comes from 
   https://github.com/pauldorman/clj-ldap and is licensed under the
   Eclipse Public License.

   This file has been modified from the original to include the 
   users and groups organizational units."
  (:require [clj-ldap.client :as ldap]
            [fs.core :as fs])
  (:import [org.apache.directory.server.core
            DefaultDirectoryService
            DirectoryService])
  (:import [org.apache.directory.server.ldap
            LdapServer])
  (:import [org.apache.directory.server.protocol.shared.transport
            TcpTransport])
  (:import [java.util HashSet])
  (:import [org.apache.directory.server.core.partition.impl.btree.jdbm
            JdbmPartition
            JdbmIndex]))

(defonce server (atom nil))

(def ldap-port 1389)
(def ldap-ssl-port 1636)

(def root-dn "dc=alienscience,dc=org,dc=uk")
(def users-dn (str "ou=users," root-dn))
(def groups-dn (str "ou=groups," root-dn))
(def user-dn-fmt (str "uid=%s," users-dn))
(def group-dn-fmt (str "cn=%s," groups-dn))

(defn- add-partition!
  "Adds a partition to the embedded directory service"
  [service id dn]
  (let [partition (doto (JdbmPartition.)
                    (.setId id)
                    (.setSuffix dn))]
    (.addPartition service partition)
    partition))

(defn- add-index!
  "Adds an index to the given partition on the given attributes"
  [partition & attrs]
  (let [indexed-attrs (HashSet.)]
    (doseq [attr attrs]
      (.add indexed-attrs (JdbmIndex. attr)))
    (.setIndexedAttributes partition indexed-attrs)))

(defn- start-ldap-server
  "Start up an embedded ldap server"
  [port ssl-port]
  (let [work-dir (fs/temp-dir "ldap-")
        directory-service (doto (DefaultDirectoryService.)
                            (.setShutdownHookEnabled true)
                            (.setWorkingDirectory work-dir))
        ldap-transport (TcpTransport. port)
        ssl-transport (doto (TcpTransport. ssl-port)
                        (.setEnableSSL true))
        ldap-server (doto (LdapServer.)
                      (.setDirectoryService directory-service)
                      ;; remove--need user checking for bind (.setAllowAnonymousAccess true)
                      (.setTransports
                        (into-array [ldap-transport ssl-transport])))]
    (-> (add-partition! directory-service "clojure" root-dn)
        (add-index! "objectClass" "ou" "uid"))
    (.startup directory-service)
    (.start ldap-server)
    [directory-service ldap-server]))

(defn- add-toplevel-objects!
  "Adds top level objects, needed for testing, to the ldap server"
  [connection]
  (ldap/add connection root-dn
            {:objectClass ["top" "domain" "extensibleObject"]
             :dc "alienscience"})
  (ldap/add connection users-dn
            {:objectClass ["top" "organizationalUnit"]
             :ou "users"})
  (ldap/add connection groups-dn
            {:objectClass ["top" "organizationalUnit"]
             :ou "groups"}))

(defn stop!
  "Stops the embedded ldap server"
  []
  (if @server
    (let [[directory-service ldap-server] @server]
      (reset! server nil)
      (.stop ldap-server)
      (.shutdown directory-service))))

(defn start!
  "Starts an embedded ldap server on defined ports"
  []
  (let [port ldap-port
        ssl-port ldap-ssl-port]
    (stop!)
    (reset! server (start-ldap-server port ssl-port))
    (let [conn (ldap/connect {:host {:address "localhost" :port port}})]
      (add-toplevel-objects! conn))))