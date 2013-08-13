(ns eu.stratuslab.cimi.resources.machine-configuration-test
  (:require
   [eu.stratuslab.cimi.resources.machine-configuration :refer :all]
   [eu.stratuslab.cimi.resources.utils :as utils]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [clojure.test :refer :all]
   [clojure.data.json :as json]
   [peridot.core :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app []
  (t/make-ring-app resource-routes))


(deftest test-crud-workflow

  ;; create
  (let [results (-> (session (ring-app))
                  (request base-uri :request-method :post))
        response (:response results)
        resource-uri (get-in response [:headers "Location"])]
    (is (= 201 (:status response)))
    (is (empty? (:body response)))
    (is (not (empty? resource-uri)))

    ;; read    
    (let [results (-> (session (ring-app))
                    (request resource-uri))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= resource-uri (get-in response [:body :id]))))
    
    ;; update    
    (let [results (-> (session (ring-app))
                    (request resource-uri :request-method :put :body (json/write-str {:name "OK"})))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= "OK" (:name (:body response)))))
    
    ;; re-read for updated entry    
    (let [results (-> (session (ring-app))
                    (request resource-uri))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= resource-uri (get-in response [:body :id])))
      (is (= "OK" (get-in response [:body :name]))))
    
    ;; delete
    (let [results (-> (session (ring-app))
                    (request resource-uri :request-method :delete))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (empty? (:body response))))
    
    ;; re-read to ensure entry is gone
    (let [results (-> (session (ring-app))
                    (request resource-uri))
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
                  (request resource-uri :request-method :put :body (json/write-str {:name "OK"})))
        response (:response results)]
    (is (= 404 (:status response)))
    (is (empty? (:body response)))))


(deftest test-create-with-body

  ;; create
  (let [results (-> (session (ring-app))
                  (request base-uri :request-method :post :body (json/write-str {:name "OK"})))
        response (:response results)
        resource-uri (get-in response [:headers "Location"])]
    (is (= 201 (:status response)))
    (is (empty? (:body response)))
    (is (not (empty? resource-uri)))

    ;; read    
    (let [results (-> (session (ring-app))
                    (request resource-uri))
          response (:response results)]
      (is (= 200 (:status response)))
      (is (= "OK" (get-in response [:body :name])))
      (is (= resource-uri (get-in response [:body :id]))))))

(defn create-with-rest [name]
  (let [results (-> (session (ring-app))
                  (request base-uri :request-method :post :body (json/write-str {:name name})))
        response (:response results)]
    (get-in response [:headers "Location"])))

(defn get-with-rest [resource-uri]
  (let [results (-> (session (ring-app))
                  (request resource-uri))
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
        body (:body response)]
      (is (= (count keys) (count body)))
      (is (= (set keys) (set (map :name body)))))

    ;; limit to half the entries and make sure only a subset is returned
    (let [limit 5
          results (-> (session (ring-app))
                    (request base-uri :body (json/write-str {:limit limit})))
        response (:response results)
        body (:body response)]
      (is (= limit (count body))))
    ))
