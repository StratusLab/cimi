(ns eu.stratuslab.cimi.main
  "Entry point for running the StratusLab CIMI interface as a
  standalone process."
  (:require [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& m]
  (let [n (symbol "eu.stratuslab.cimi.server")
        init (symbol "init")
        servlet-handler (symbol "servlet-handler")]
    (require n)
    (let [init-fn (ns-resolve (the-ns n) init)
          handler (ns-resolve (the-ns n) servlet-handler)]
      (init-fn "")
      (run-jetty handler {:port 8080 :join? false}))))
