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

(ns eu.stratuslab.cimi.resources.auth-utils
  "Utilities for dealing with authn and authz decisions"
  (:require
    [ring.util.response :as r]
    [cemerick.friend :as friend]))

(defn owner-type
  "Returns the type of the owner in the given ACL as a keyword.
   Will return nil if the owner type is missing or an invalid
   value.  Only 'USER' and 'ROLE' are accepted."
  [acl]
  (-> acl
      (get-in [:owner :type])
      (keyword)
      (#{:USER :ROLE})))

(defn owner?
  "Determines if the user with the given authn information is the
   owner of the resource with the given ACL.  Returns the name of
   the owner if true; returns nil otherwise."
  [authn acl]
  (if-let [owner (get-in acl [:owner :principal])]
    (case (owner-type acl)
      :USER (if (= owner (:identity authn)) owner)
      :ROLE ((or (set (:roles authn)) #{}) owner)
      nil)))

(defn authn->ids
  "Provides a list of IDs suitable for comparing against a set
   of rules.  The IDs are maps with the :principal and :type
   fields.  This returns a sequence of IDs or nil."
  [authn]
  (if-let [id (:identity authn)]
    (set (-> (or (map (fn [r] {:principal r :type "ROLE"}) (:roles authn)) [])
             (conj {:principal id :type "USER"})
             (conj {:principal "::USER" :type "ROLE"})
             (conj {:principal "::ANON" :type "ROLE"})))
    (set [{:principal "::ANON" :type "ROLE"}])))

(defn authn-id-matches?
  "Determines if a single authentication ID matches the given rule.
   To match, the :principal and :type must be the same."
  [rule authn-id]
  (and (= (:type authn-id) (:type rule))
       (= (:principal authn-id) (:principal rule))))

(defn rule-matches?
  "Returns true if the rule matches one or more of the authentication IDs."
  [authn-ids rule]
  (some true? (map (partial authn-id-matches? rule) authn-ids)))

(defn access-right
  "Given a friend authentication map and an ACL, the function returns the
   access right for the user.  If multiple rules in the ACL match, then
   the most permissive allowed permission will be returned.  The value
   is either :ALL, :MODIFY, :VIEW or nil."
  [authn acl]
  (if (owner? authn acl)
    :ALL
    (if-let [authn-ids (authn->ids authn)]
      (if-let [rights (->> (:rules acl)
                           (filter (partial rule-matches? authn-ids))
                           (map :right)
                           (set))]
        (->> (map #(get rights %) ["ALL" "MODIFY" "VIEW"])
             (remove nil?)
             (first)
             (keyword))))))

(defn can-view? [authn acl]
  (#{:VIEW :MODIFY :ALL} (access-right authn acl)))

(defn can-modify? [authn acl]
  (#{:MODIFY :ALL} (access-right authn acl)))

(defn can-modify-acl? [authn acl]
  (#{:ALL} (access-right authn acl)))

