;; FIXME: this file is causing the build to hang when done from the top
;; authn level.
(ns eu.stratuslab.authn.vm-rest.main
  "Entry point for running the VM REST API as a standalone process."
  (:require [noir.server :as noir])
  (:gen-class))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))
        server-options {:mode mode :ns 'eu.stratuslab.authn.vm-rest.main}]

    (let [n (symbol "eu.stratuslab.authn.vm-rest.server")
          init (symbol "init")]
      (require n)
      (let [init-fn (ns-resolve (the-ns n) init)]
        (init-fn "")
        (noir/start port server-options)))))
