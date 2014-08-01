(ns eu.stratuslab.cimi.cb.bootstrap
  "Provides the utility to provide the necessary views and objects
   in the Couchbase database for minimal operation of the CIMI
   service."

  (:require
    [clojure.tools.logging :as log]
    [eu.stratuslab.cimi.resources.cloud-entry-point :as cep]
    [eu.stratuslab.cimi.resources.user :as user]
    [eu.stratuslab.cimi.cb.views :as views]
    [eu.stratuslab.cimi.cb.utils :as cbutils]
    [couchbase-clj.client :as cbc]
    [cemerick.friend.credentials :as creds]))

(defn create-views
  "Ensure that the views necessary for searching the database
   are available."
  [cb-client]
  (if (views/add-design-doc cb-client)
    (log/info "design document added to database")
    (log/info "design document NOT added to database")))

(defn create-cep
  "Checks to see if the CloudEntryPoint exists in the database;
   if not, it will create one.  The CloudEntryPoint is the core
   resource of the service and must exist."
  [cb-client]
  (if (cep/add cb-client)
    (log/info "created CloudEntryPoint")
    (do
      (log/warn "did NOT create CloudEntryPoint")
      (let [status (:status (cep/retrieve cb-client "http://example.org/"))]
        (if (= 200 status)
          (log/info "CloudEntryPoint exists")
          (log/error "problem retrieving CloudEntryPoint"))))))

(defn random-password
  "A random password of 12 characters consisting of the ASCII
   characters between 40 '(' and 95 '_'."
  []
  (let [valid-chars (map char (concat (range 48 58)
                                      (range 65 91)
                                      (range 97 123)))]
    (reduce str (for [_ (range 12)] (rand-nth valid-chars)))))

(defn create-admin
  "Checks to see if the User/admin entry exists in the database;
   if not, it will create one with a randomized password.  The
   clear text password will be written to the service log."
  [cb-client]
  (try
    (let [password (random-password)
          admin {:first-name "cloud"
                 :last-name  "administrator"
                 :username   "admin"
                 :password   (creds/hash-bcrypt password)
                 :enabled    true
                 :roles      ["::ADMIN"]
                 :email      "change_me@example.com"}]
      (if (= 201 (:status (user/add cb-client admin)))
        (log/warn "User/admin entry created; initial password is" password)
        (log/info "User/admin entry NOT created")))
    (catch Exception e
      (log/error "Error occurred while trying to create User/admin entry" (str e)))))

(defn bootstrap
  "Bootstraps the Couchbase database by creating the CloudEntryPoint
   and inserting the design document with views, as necessary."
  [cb-client]
  (cbutils/wait-until-ready cb-client)
  (create-views cb-client)
  (create-admin cb-client)
  (create-cep cb-client)
  (views/views-available? cb-client))
