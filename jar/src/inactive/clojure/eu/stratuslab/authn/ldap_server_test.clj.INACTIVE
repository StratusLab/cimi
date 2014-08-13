(ns eu.stratuslab.authn.ldap-server-test
  "Creates an embedded LDAP server for unit testing.  This server
   comes from the ApacheDS project.

   The code for the embedded server is a mix of code from
   https://github.com/pauldorman/clj-ldap (Eclipse Public License)
   updated with the ideas from https://github.com/soluvas/dsembed so
   that it works with ApacheDS 2."
  (:require
    [clj-ldap.client :as ldap]
    [fs.core :as fs])
  (:import
    [org.apache.directory.api.ldap.model.name Dn]
    [org.apache.directory.api.ldap.model.schema SchemaManager]
    [org.apache.directory.api.ldap.schemaextractor.impl DefaultSchemaLdifExtractor]
    [org.apache.directory.api.ldap.schemaloader LdifSchemaLoader]
    [org.apache.directory.api.ldap.schemamanager.impl DefaultSchemaManager]
    [org.apache.directory.server.constants ServerDNConstants]
    [org.apache.directory.server.core.api DirectoryService InstanceLayout]
    [org.apache.directory.server.core.api.schema SchemaPartition]
    [org.apache.directory.server.core DefaultDirectoryService]
    [org.apache.directory.server.core.partition.impl.btree.jdbm JdbmPartition JdbmIndex]
    [org.apache.directory.server.core.partition.ldif LdifPartition]
    [org.apache.directory.server.core.shared DefaultDnFactory]
    [org.apache.directory.server.ldap LdapServer]
    [org.apache.directory.server.protocol.shared.transport TcpTransport]
    [net.sf.ehcache CacheManager Cache]
    [java.io File]
    [java.util HashSet]))

(defonce server (atom nil))

(def ldap-port 1389)
(def ldap-ssl-port 1636)

(def root-dn "dc=alienscience,dc=org,dc=uk")
(def users-dn (str "ou=users," root-dn))
(def groups-dn (str "ou=groups," root-dn))
(def user-dn-fmt (str "uid=%s," users-dn))
(def group-dn-fmt (str "cn=%s," groups-dn))

(defn- get-schema-dir
  [work-dir]
  (File. work-dir "schema"))

(defn- create-dn-factory
  [schema-mgr name]
  (let [cache (Cache. name 10000 false false 1000 1000)]
    (DefaultDnFactory. schema-mgr cache)))

(defn- create-partition
  "adds a partition to the service with the given id and
   dn located in a subdirectory of the path; returns the
   partition"
  [service work-dir id dn]
  (let [schema-mgr (.getSchemaManager service)
        path (.toURI (File. work-dir id))
        dn (Dn. (into-array String [dn]))
        dn-factory (create-dn-factory schema-mgr "ldap")]
    (doto (JdbmPartition. schema-mgr dn-factory)
      (.setId id)
      (.setPartitionPath path)
      (.setSuffixDn dn))))

(defn- add-partition!
  "adds a partition to the service with the given id and
   dn located in a subdirectory of the path; returns the
   partition"
  [service work-dir id dn]
  (let [partition (create-partition service work-dir id dn)]
    (.addPartition service partition)
    partition))

(defn- add-system-partition!
  "creates a partition from the parameters and sets this as
   a system partition on the directory service; function
   returns nil"
  [service work-dir id dn]
  (->> (create-partition service work-dir id dn)
       (.setSystemPartition service)))

(defn- add-index!
  "Adds an index to the given partition on the given attributes"
  [partition & attrs]
  (let [indexed-attrs (HashSet.)]
    (doseq [attr attrs]
      (.add indexed-attrs (JdbmIndex. attr false)))
    (.setIndexedAttributes partition indexed-attrs)))

(defn- init-ldif-partition
  [schema-mgr work-dir]
  (let [schema-dir (get-schema-dir work-dir)
        dn-factory (create-dn-factory schema-mgr "ldif")
        ldif-partition (doto (LdifPartition. schema-mgr dn-factory)
                         (.setPartitionPath (.toURI schema-dir)))]
    (if-not (.exists schema-dir)
      (doto (DefaultSchemaLdifExtractor. work-dir)
        (.extractOrCopy true))) ;; overwrites any existing files
    ldif-partition))

(defn- extract-schema
  [service work-dir]
  (let [schema-mgr (.getSchemaManager service)
        schema-partition (SchemaPartition. schema-mgr)
        _ (.setSchemaPartition service schema-partition)

        ldif-partition (init-ldif-partition schema-mgr work-dir)]
    (.setWrappedPartition schema-partition ldif-partition))
  nil)

(defn- init-schema-partition
  [service work-dir]
  (extract-schema service work-dir)
  (let [schema-mgr (.getSchemaManager service)
        loader (LdifSchemaLoader. (get-schema-dir work-dir))]
    (.setSchemaLoader schema-mgr loader)
    (.loadAllEnabled schema-mgr)))

(defn- init-directory-service
  [work-dir]
  (let [service (doto (DefaultDirectoryService.)
                  (.setSchemaManager (DefaultSchemaManager.))
                  (.setInstanceLayout (InstanceLayout. work-dir)))]
    (init-schema-partition service work-dir)
    (add-system-partition! service work-dir "system" ServerDNConstants/SYSTEM_DN)
    (.. service
        (getChangeLog)
        (setEnabled false))

    (doto service
      (.setDenormalizeOpAttrsEnabled true)
      (.setAllowAnonymousAccess true)
      (.setAccessControlEnabled false))

    (-> (add-partition! service work-dir "clojure" root-dn)
        (add-index! "objectClass" "ou" "uid"))
    service))

(defn- start-ldap-server
  "Start up an embedded ldap server"
  [port ssl-port]
  (let [work-dir (fs/temp-dir "ldap-")
        directory-service (init-directory-service work-dir)
        ldap-transport (TcpTransport. port)
        ssl-transport (doto (TcpTransport. ssl-port)
                        (.setEnableSSL true))
        transports (into-array [ldap-transport ssl-transport])
        ldap-server (doto (LdapServer.)
                      (.setDirectoryService directory-service)
                      (.setTransports transports))]
    (.startup directory-service)
    (.start ldap-server)
    [directory-service ldap-server]))

(defn- add-toplevel-objects!
  "Adds top level objects, needed for testing, to the ldap server"
  []
  (let [connection (ldap/connect {:bind-dn  "uid=admin,ou=system"
                                  :password "secret"
                                  :host     {:address "localhost"
                                             :port    ldap-port}})]
    (ldap/add connection root-dn
              {:objectClass ["top" "domain" "extensibleObject"]
               :dc          "alienscience"})
    (ldap/add connection users-dn
              {:objectClass ["top" "organizationalUnit"]
               :ou          "users"})
    (ldap/add connection groups-dn
              {:objectClass ["top" "organizationalUnit"]
               :ou          "groups"})))

(defn stop!
  "stops the ldap server"
  []
  (if-let [s @server]
    (let [[directory-service ldap-server] s]
      (reset! server nil)
      (.shutdown directory-service)
      (.stop ldap-server))))

(defn start!
  "starts the ldap server, stopping it if it is already running"
  []
  (stop!)
  (reset! server (start-ldap-server ldap-port ldap-ssl-port))
  (add-toplevel-objects!))
