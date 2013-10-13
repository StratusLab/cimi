;
; Copyright 2013 Centre National de la Recherche Scientifique (CNRS)
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns eu.stratuslab.cimi.resources.auth-utils-test
  (:require [eu.stratuslab.cimi.resources.auth-utils :refer :all]
            [eu.stratuslab.cimi.couchbase-test-utils :as t]
            [clojure.test :refer :all]
            [couchbase-clj.client :as cbc])
  (:import [java.util UUID]))

(deftest check-owner-type
  (are [e correct] (= correct (owner-type e))
                   {:owner {:type "USER"}} :USER
                   {:owner {:type "ROLE"}} :ROLE
                   {:owner {:type "user"}} nil
                   {:owner {:type "role"}} nil
                   {:owner {:type "BAD"}} nil
                   nil nil))

(deftest check-owner?
  (let [authn {:identity "user"
               :roles #{"group1" "group2" "group3"}}]
    (are [correct acl] (= correct (owner? authn acl))
                       "user" {:owner {:principal "user"
                                       :type "USER"}}
                       nil {:owner {:principal "bad"
                                    :type "USER"}}
                       nil {:owner {:principal "group1"
                                    :type "USER"}}
                       "group1" {:owner {:principal "group1"
                                         :type "ROLE"}}
                       "group2" {:owner {:principal "group2"
                                         :type "ROLE"}}
                       nil {:owner {:principal "user"
                                    :type "ROLE"}}
                       nil {:owner {:principal "bad"
                                    :type "ROLE"}}
                       nil {:owner {:principal "user"
                                    :type "BAD"}}
                       nil {:owner {}}))

  (let [authn {:identity "user"
               :roles ["group1" "group2" "group3"]}]
    (are [correct acl] (= correct (owner? authn acl))
                       "user" {:owner {:principal "user"
                                       :type "USER"}}
                       nil {:owner {:principal "bad"
                                    :type "USER"}}
                       nil {:owner {:principal "group1"
                                    :type "USER"}}
                       "group1" {:owner {:principal "group1"
                                         :type "ROLE"}}
                       "group2" {:owner {:principal "group2"
                                         :type "ROLE"}}
                       nil {:owner {:principal "user"
                                    :type "ROLE"}}
                       nil {:owner {:principal "bad"
                                    :type "ROLE"}}
                       nil {:owner {:principal "user"
                                    :type "BAD"}}
                       nil {:owner {}}))

  (let [authn {:roles #{"group1" "group2" "group3"}}]
    (are [correct acl] (= correct (owner? authn acl))
                       nil {:owner {:principal "user"
                                    :type "USER"}}
                       nil {:owner {:principal "bad"
                                    :type "USER"}}
                       nil {:owner {:principal "group1"
                                    :type "USER"}}))

  (let [authn {:identity "user"}]
    (are [correct acl] (= correct (owner? authn acl))
                       "user" {:owner {:principal "user"
                                       :type "USER"}}
                       nil {:owner {:principal "group1"
                                    :type "ROLE"}}
                       nil {:owner {:principal "group2"
                                    :type "ROLE"}}))


  (let [authn {}]
    (are [correct acl] (= correct (owner? authn acl))
                       nil {:owner {:principal "user"
                                    :type "USER"}}
                       nil {:owner {:principal "group1"
                                    :type "ROLE"}})))


(deftest check-authn-ids
  (are [authn correct] (= correct (authn->ids authn))
                       nil
                       #{{:principal "::ANON"
                          :type "ROLE"}}

                       {}
                       #{{:principal "::ANON"
                          :type "ROLE"}}

                       {:identity "me"}
                       #{{:principal "::ANON"
                          :type "ROLE"}
                         {:principal "::USER"
                          :type "ROLE"}
                         {:principal "me"
                          :type "USER"}}

                       {:identity "me"
                        :roles ["group1" "group2"]}
                       #{{:principal "::ANON"
                          :type "ROLE"}
                         {:principal "::USER"
                          :type "ROLE"}
                         {:principal "me"
                          :type "USER"}
                         {:principal "group1"
                          :type "ROLE"}
                         {:principal "group2"
                          :type "ROLE"}}))

(deftest check-authn-id-matches?
  (let [rule {:principal "ALPHA"
              :type "USER"
              :right "VIEW"}]
    (are [correct authn-id] (= correct (authn-id-matches? authn-id rule))
                            true {:principal "ALPHA" :type "USER"}
                            false {:principal "ALPHA" :type "ROLE"}
                            false {:principal "ALPHA"}
                            false {:type "ROLE"}
                            false {}))

  (let [rule {:principal "BETA"
              :type "ROLE"
              :right "MODIFY"}]
    (are [correct authn-id] (= correct (authn-id-matches? authn-id rule))
                            false {:principal "BETA" :type "USER"}
                            true {:principal "BETA" :type "ROLE"}
                            false {:principal "BETA"}
                            false {:type "USER"}
                            false {})))

(deftest check-rule-matches?
  (let [rule {:principal "ALPHA"
              :type "USER"
              :right "VIEW"}]
    (is (rule-matches? [{:principal "group1" :type "ROLE"}
                        {:principal "beta" :type "USER"}
                        {:principal "ALPHA" :type "USER"}]
                       rule))

    (is (not (rule-matches? [{:principal "group1" :type "ROLE"}
                             {:principal "beta" :type "USER"}
                             {:principal "ALPHA" :type "ROLE"}]
                            rule)))))

(deftest check-access-right
  (let [authn {:identity "ALPHA"
               :roles ["group1" "group2" "group3"]}]
    (are [correct acl] (= correct (access-right authn acl))
                       :ALL
                       {:owner {:principal "ALPHA"
                                :type "USER"}}

                       nil
                       {:owner {:principal "BETA"
                                :type "USER"}}

                       :VIEW
                       {:owner {:principal "BETA"
                                :type "USER"}
                        :rules [{:principal "ALPHA"
                                 :type "USER"
                                 :right "VIEW"}]}

                       :MODIFY
                       {:owner {:principal "BETA"
                                :type "USER"}
                        :rules [{:principal "ALPHA"
                                 :type "USER"
                                 :right "MODIFY"}]}

                       :ALL
                       {:owner {:principal "BETA"
                                :type "USER"}
                        :rules [{:principal "ALPHA"
                                 :type "USER"
                                 :right "ALL"}]}

                       nil
                       {:owner {:principal "BETA"
                                :type "USER"}
                        :rules [{:principal "ALPHA"
                                 :type "USER"
                                 :right "BAD"}]}

                       :VIEW
                       {:owner {:principal "BETA"
                                :type "USER"}
                        :rules [{:principal "group1"
                                 :type "ROLE"
                                 :right "VIEW"}]}

                       :MODIFY
                       {:owner {:principal "BETA"
                                :type "USER"}
                        :rules [{:principal "group1"
                                 :type "ROLE"
                                 :right "VIEW"}
                                {:principal "group2"
                                 :type "ROLE"
                                 :right "MODIFY"}]})))
