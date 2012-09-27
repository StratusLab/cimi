(ns eu.stratuslab.cimi.main
  "Entry point for running the VM REST API as a standalone process."
  (:require [noir.server :as noir])
  (:gen-class))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))
        server-options {:mode mode :ns 'eu.stratuslab.cimi.main}]

    (let [n (symbol "eu.stratuslab.cimi.server")
          init (symbol "init")]
      (require n)
      (let [init-fn (ns-resolve (the-ns n) init)]
        (init-fn "")
        (noir/start port server-options)))))
