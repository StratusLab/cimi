(ns eu.stratuslab.cimi.resources.machine-configuration-test
  (:require
   [eu.stratuslab.cimi.resources.machine-configuration :refer :all]
   [eu.stratuslab.cimi.resources.utils :as utils]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [clj-schema.validation :refer [validation-errors]]
   [clojure.test :refer :all]
   [clojure.data.json :as json]
   [peridot.core :refer :all]))

(use-fixtures :each t/flush-bucket-fixture)

(use-fixtures :once t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))

(def valid-entry
  {:name "valid"
   :description "valid machine configuration"
   :cpu 1
   :memory 512000
   :cpuArch "x86_64"
   :disks [{:capacity 1024
            :format "ext4"
            :initialLocation "/dev/hda"}]} )

(deftest test-disk-schema
  (let [disk {:capacity 1024 :format "ext4" :initialLocation "/dev/hda"}]
    (is (empty? (validation-errors Disk disk)))
    (is (empty? (validation-errors Disk (dissoc disk :initialLocation))))
    (is (not (empty? (validation-errors Disk (dissoc disk :capacity)))))
    (is (not (empty? (validation-errors Disk (dissoc disk :format)))))
    (is (not (empty? (validation-errors Disk {})))))
)

(deftest test-disks-schema
  (let [disks [{:capacity 1024 :format "ext4" :initialLocation "/dev/hda"}
               {:capacity 2048 :format "swap" :initialLocation "/dev/hdb"}]]
    (is (empty? (validation-errors Disks disks)))
    (is (empty? (validation-errors Disks (rest disks))))
    (is (not (empty? (validation-errors Disks []))))))

(deftest test-machine-configuration-schema
  (let [mc (assoc valid-entry
             :id "/MachineConfiguration/10"
             :resourceURI type-uri
             :created "1964-08-25T10:00:00.0Z"
             :updated "1964-08-25T10:00:00.0Z"
             :disks [{:capacity 1024
                      :format "ext4"}])]
        (is (empty? (validation-errors MachineConfiguration mc)))
        (is (empty? (validation-errors MachineConfiguration (dissoc mc :disks))))
        (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :cpu)))))
        (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :memory)))))
        (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :cpuArch)))))
        (is (not (empty? (validation-errors MachineConfiguration (dissoc mc :cpu)))))))

(deftest test-crud-workflow

  ;; create
  (let [results (-> (session (ring-app))
                  (request base-uri :request-method :post
                    :body (json/write-str valid-entry)))
        response (:response results)
        resource-uri (get-in response [:headers "Location"])
        resource-url (str "/" resource-uri)]
    (is (= 201 (:status response)))
    (is (empty? (:body response)))
    (is (not (empty? resource-uri)))

    ;; read    
    (let [results (-> (session (ring-app))
                    (request resource-url))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= resource-uri (get-in response [:body :id]))))
    
    ;; update    
    (let [results (-> (session (ring-app))
                    (request resource-url :request-method :put
                      :body (json/write-str {:name "OK"})))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= "OK" (:name (:body response)))))
    
    ;; re-read for updated entry    
    (let [results (-> (session (ring-app))
                    (request resource-url))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= resource-uri (get-in response [:body :id])))
      (is (= "OK" (get-in response [:body :name]))))
    
    ;; delete
    (let [results (-> (session (ring-app))
                    (request resource-url :request-method :delete))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (empty? (:body response))))
    
    ;; re-read to ensure entry is gone
    (let [results (-> (session (ring-app))
                    (request resource-url))
          response (:response results)]
      (is (= 404 (:status response)))
      (is (empty? (:body response)))) ))


(deftest read-non-existing-resource-fails
  (let [resource-uri (str base-uri "/" (utils/create-uuid))
        results (-> (session (ring-app))
                  (request resource-uri))
        response (:response results)]
    (is (= 404 (:status response)))
    (is (empty? (:body response)))))


(deftest delete-non-existing-resource-fails
  (let [resource-uri (str base-uri "/" (utils/create-uuid))
        results (-> (session (ring-app))
                  (request resource-uri :request-method :delete))
        response (:response results)]
    (is (= 404 (:status response)))
    (is (empty? (:body response)))))
    

(deftest update-non-existing-resource-fails
  (let [resource-uri (str base-uri "/" (utils/create-uuid))
        results (-> (session (ring-app))
                  (request resource-uri :request-method :put
                    :body (json/write-str {:name "OK"})))
        response (:response results)]
    (is (= 404 (:status response)))
    (is (empty? (:body response)))))


(defn create-with-rest [name]
  (let [results (-> (session (ring-app))
                  (request base-uri :request-method :post
                    :body (json/write-str (assoc valid-entry :name name))))
        response (:response results)]
    (get-in response [:headers "Location"])))

(defn get-with-rest [resource-uri]
  (let [results (-> (session (ring-app))
                  (request (str "/" resource-uri)))
        response (:response results)]
    (:body response)))

(deftest test-queries

  ;; creates number of entries in database with index as name
  (let [keys (map str (range 10))
        values (map create-with-rest keys)
        m (zipmap keys values)
        bodies (map get-with-rest values)
        names (map :name bodies)]   
    (is (= keys names))
    
    ;; ensure that all of the entries are present
    (let [results (-> (session (ring-app))
                    (request base-uri))
        response (:response results)
        body (:body response)
        docs (:machineConfigurations body)]
      (is (= collection-type-uri (:resourceURI body)))
      (is (= base-uri (:id body)))
      (is (= (count keys) (:count body)))
      (is (= (count keys) (count docs)))
      (is (= (set keys) (set (map :name docs)))))

    ;; limit to half the entries and make sure only a subset is returned
    (let [limit 5
          results (-> (session (ring-app))
                    (request base-uri :body (json/write-str {:limit limit})))
        response (:response results)
        body (:body response)
        docs (:machineConfigurations body)]
      (is (= collection-type-uri (:resourceURI body)))
      (is (= base-uri (:id body)))
      (is (= limit (:count body)))
      (is (= limit (count docs))))))
