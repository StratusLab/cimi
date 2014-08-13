(ns eu.stratuslab.cimi.app.couchbase-cfg-test
  (:require
    [eu.stratuslab.cimi.app.couchbase-cfg :refer :all]
    [clojure.test :refer [deftest are is]])
  (:import
    [java.net URI]
    [java.io StringReader]))

(deftest check-hostport-conversions
  (are [hostport correct] (= correct (hostport->uri hostport))
                          nil nil
                          "" nil
                          ":1024" nil
                          "localhost:" nil
                          "localhost:bad" nil
                          "localhost:2bad" nil
                          "localhost" (URI/create "http://localhost:8091/pools")
                          "localhost:2456" (URI/create "http://localhost:2456/pools")
                          "onehost5" (URI/create "http://onehost5:8091/pools")))

(deftest check-hostports-conversions
  (are [hostports correct] (= correct (hostports->uris hostports))
                           nil [(URI/create "http://localhost:8091/pools")]
                           "bad:port" []
                           "host:8091" [(URI/create "http://host:8091/pools")]
                           "   host1 host2:2222 host3:bad :awful yuck:" [(URI/create "http://host1:8091/pools")
                                                                         (URI/create "http://host2:2222/pools")]
                           ))

(deftest check-get-value
  (let [ini-map {:alpha 1
                 :beta 2
                 :gamma 3
                 :DEFAULT {:beta 4
                           :gamma 5}
                 :cimi {:gamma 6}}]
    (is (nil? (get-value ini-map :delta)))
    (are [name correct] (= correct (get-value ini-map name 10))
                        :alpha 1
                        :beta 4
                        :gamma 6
                        :delta 10)))

(deftest check-read-cfg
  (let [input (StringReader. "")]
    (is (= {:uris [(URI/create "http://localhost:8091/pools")]
            :bucket "default"
            :username ""
            :password ""} (read-cfg input))))

  (let [input (StringReader. "host= host1 host2:9999\nbucket=b\nusername=u\npassword=p\n")]
    (is (= {:uris [(URI/create "http://host1:8091/pools") (URI/create "http://host2:9999/pools")]
            :bucket "b"
            :username "u"
            :password "p"} (read-cfg input)))))
