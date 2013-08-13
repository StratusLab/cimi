(ns eu.stratuslab.cimi.resources.machine-configuration-test
  (:require
   [eu.stratuslab.cimi.resources.machine-configuration :refer :all]
   [eu.stratuslab.cimi.cb.bootstrap :refer [bootstrap]]
   [eu.stratuslab.cimi.couchbase-test-utils :as t]
   [eu.stratuslab.cimi.middleware.cb-client :refer [wrap-cb-client]]
   [clojure.test :refer :all]
   [clojure.data.json :as json]
   [peridot.core :refer :all]))

(use-fixtures :each t/temp-bucket-fixture)

(defn ring-app [cb-client]
  (bootstrap cb-client)
  (wrap-cb-client cb-client resource-routes))

(deftest test-crud-workflow

  ;; create
  (let [results (-> (session (ring-app t/*test-cb-client*))
                  (request base-uri :request-method :post))
        response (:response results)
        resource-uri (get-in response [:headers "Location"])]
    (println response)
    (is (= 201 (:status response)))
    (is (empty? (:body response)))
    (is (not (empty? resource-uri)))

    ;; read    
    (let [results (-> (session (ring-app t/*test-cb-client*))
                    (request resource-uri))
          response (:response results)]
      (println resource-uri)
      (println response)
      (is (= 200 (:status response)))
      (is (= resource-uri (get-in response [:body :id]))))
    
    ;; update    
    (let [results (-> (session (ring-app t/*test-cb-client*))
                    (request resource-uri :request-method :put :body (json/write-str {:name "OK"})))
          response (:response results)]
      (println resource-uri)
      (println response)
      (is (= 200 (:status response)))
      (is (= "OK" (:name (:body response)))))
    
    ;; re-read for updated entry    
    (let [results (-> (session (ring-app t/*test-cb-client*))
                    (request resource-uri))
          response (:response results)]
      (println resource-uri)
      (println response)
      (is (= 200 (:status response)))
      (is (= resource-uri (get-in response [:body :id])))
      (is (= "OK" (get-in response [:body :name]))))
    
    ;; delete
    (let [results (-> (session (ring-app t/*test-cb-client*))
                    (request resource-uri :request-method :delete))
          response (:response results)]
      (println response)
      (is (= 200 (:status response)))
      (is (empty? (:body response))))
    
    ;; re-read to ensure entry is gone
    (let [results (-> (session (ring-app t/*test-cb-client*))
                    (request resource-uri))
          response (:response results)]
      (println response)
      (is (= 404 (:status response)))
      (is (empty? (:body response))))
    
    ;; delete non-existent resource returns not found status
    (let [results (-> (session (ring-app t/*test-cb-client*))
                    (request resource-uri :request-method :delete))
          response (:response results)]
      (println response)
      (is (= 404 (:status response)))
      (is (empty? (:body response)))) ))
