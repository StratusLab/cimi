(ns eu.stratuslab.cimi.utils.filter
  (:require [com.lithinos.amotoen.core :refer [pegs lpegs]]))

(def ^:const digits "0123456789")

(def ^:const letters "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_")

(def ^:const operators ["<" "<=" "=" ">=" ">" "!="])

(defn as-string [v]
  (apply str (doall v)))

(defn literal [s]
  (list 'f #(apply str %) (pegs s)))

(defn digits-to-string [digit & others]
  true)

;;
;; There are a couple of ambiguities in the standard for the filter
;; syntax.  First, an attribute name can appear next to a literal
;; 'and' or 'or' leading to an ambiguity as to where the attribute
;; name starts or ends.  Second, there is no way to distinguish
;; integer and date values that use the compact format and no time.
;;
(def filter-grammar
     {
      :Filter [:AndExpr (list '* [(literal "or") :Filter]) :$]
      :AndExpr [:Comp (list '* [(literal "and") :AndExpr])]
      :Comp '(| [:Attribute :Op :Value]
                [:Value :Op :Attribute]
                [\( :Filter \)])
      :Op (apply list '| (map pegs operators))
      :Attribute '(| :Name :PropExpr)
      
      :Value '(| :IntValue :StringValue :BoolValue)

      :IntValue [:digit '(* :digit)]
      :StringValue '(| :double-quoted-string :single-quoted-string)
      :BoolValue (list '|
                       (literal "true")
                       (literal "false"))

      :Name [:alpha '(* :alphanum)]

      :PropExpr [(literal "property[") :StringValue (literal "]") :Op :StringValue]

      :double-quoted-string [\" '(| [\\ \"] [\\ \\] (% \")) \"]
      :single-quoted-string [\' '(| [\\ \'] [\\ \\] (% \')) \']
      
      :digit (lpegs '| digits)
      :alpha (lpegs '| letters)
      :alphanum '(| :alpha :digit)})
