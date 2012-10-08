(ns eu.stratuslab.cimi.utils.filter
  (:require [com.lithinos.amotoen.core :refer [pegs lpegs]]
            [clojure.string :as str]))

(def ^:const digits "0123456789")

(def ^:const letters "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_")

(def ^:const alphanum (str digits letters))

(def ^:const operators ["<" "<=" "=" ">=" ">" "!="])

(defn as-string [v]
  (apply str (doall v)))

(defn literal [s]
  (list 'f #(apply str %) (pegs s)))

(defn digits-to-string [digit & others]
  true)

(defn concat-chars [v]
  (if v (apply str v)))

(defn ignore [_] "")

(defn value-as-string [v]
  (apply str (map #(first (vals %)) (flatten  v))))

(defn process-string-value [v]
  (let [v (if (seq? v) (flatten v) [v])]
    (apply str v)))

(defn process-string [v]
  (first (vals (first (filter map? v)))))

(defn process-op [v]
  (let [op (apply str v)]
    (if (= op "!=")
      "not="
      op)))

(defn process-propexpr [v]
  (println v)
  (let [args (filter map? v)
        _ (println args)
        [attr op value] (map #(first (vals %)) args)]
    (format "(%s (get properties \"%s\") \"%s\")" op attr value)))

(defn take-value [v]
  v)

;;
;; There are a couple of ambiguities in the standard for the filter
;; syntax.  First, an attribute name can appear next to a literal
;; 'and' or 'or' leading to an ambiguity as to where the attribute
;; name starts or ends.  Second, there is no way to distinguish
;; integer and date values that use the compact format and no time.
;;
(def filter-grammar
     {
      :Filter [:AndExpr '(* [:or :AndExpr])]
      :AndExpr [:Comp '(* [:and :AndExpr])]
      :Comp '(| [\( :Filter \)]
                [:Attribute :Op :Value]
                [:Value :Op :Attribute])
      :Op (apply list '| (map pegs operators))
      :Attribute '(| :Name :PropExpr)
      
      :Value '(| :IntValue :StringValue :BoolValue)

      :IntValue [:digit '(* :digit)]
      :StringValue '(| [\" :double-quoted-value \"]
                       [\' :single-quoted-value \'])
      :BoolValue (list 'f concat-chars (list '| (pegs "true") (pegs "false")))

      :Name [:alpha '(* :alphanum)]

      :PropExpr [:prefix :StringValue :suffix]

      :double-quoted-value '(* (| [\\ \"] [\\ \\] (% \")))
      :single-quoted-value '(* (| [\\ \'] [\\ \\] (% \')))

      :digit (lpegs '| digits)
      :alpha (lpegs '| letters)
      :alphanum (lpegs '| alphanum)

      :or (pegs "or")
      :and (pegs "and")
      :prefix (pegs "property[")
      :suffix (pegs "]")
      })

(def to-clj-fns
     {:IntValue value-as-string
      :BoolValue take-value
      :StringValue process-string
      :Name value-as-string
      :Op process-op
      :PropExpr process-propexpr
      :single-quoted-value process-string-value
      :double-quoted-value process-string-value})

(def test-fns
     {:or ignore
      :and ignore
      :prefix ignore
      :suffix ignore})