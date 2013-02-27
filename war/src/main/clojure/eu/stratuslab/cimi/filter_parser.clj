(ns eu.stratuslab.cimi.filter-parser
  "Implements a parser for CIMI filters as defined in Section 4.1.6.1 of the 
  CIMI specification (DSP0263 v1.0.1)."
  (:require
    [clojure.edn :as edn]
    [blancas.kern.core :refer :all]
    [blancas.kern.i18n :refer [i18n]]
    [blancas.kern.expr :refer :all]
    [blancas.kern.lexer.basic :refer :all]))

;; TODO: Ensure that no trailing input is left at end of parsing
;; TODO: Determine mechanism for signaling an exception to the parser for dates

(declare Filter)

(def datetime-lit
  "A token that conforms to the XML Schema Date and DateTime format (ISO 8601).
   The default reader instant is returned for the value."
  (<?>
    (>>= (<:> (lexeme
                   (<+>
                     (many1 digit)
                     (sym \-)
                     (many1 digit)
                     (sym \-)
                     (many1 digit)
                     (optional 
                       (<+>
                         (sym \T)
                         (many1 digit)
                         colon
                         (many1 digit)
                         colon
                         (many1 digit)
                         (optional (<+>
                                     (sym \.) 
                                     (many1 digit)))
                         (<|>
                           (sym \Z)
                           (<+>
                             (one-of* "+-")
                             (many1 digit)
                             colon
                             (many1 digit)))))
                     )))
         (fn [x] (return (edn/read-string (str "#inst \"" x "\"")))))
    (i18n (fn [x] "expecting DateTime value"))))

(def sq-string-lit
  "Parses string literals delimited by single quotes. Note that this 
   implementation calls the private basic-char function in Kern."
  (<?>
    (between
      (sym* \')
      (<?> (sym* \') (i18n :end-string))
      (<+> (many (#'blancas.kern.lexer/basic-char \'))))
    (i18n :string-lit)))

(def Op
  "Parses one of the relational operators."
  (bind [op (token "<" "<=" "=" ">=" ">" "!=")]
    (return ({"<" < "<=" <= "=" = ">=" >= ">" > "!=" not=} op))))

(def RestrictedOp
  "For strings only equality and inequality are permitted."
  (bind [op (token "=" "!=")]
    (return ({"=" = "!=" not=} op))))

(def And
  "Parses the logical 'and' operator."
  (>> (token "and") (return #(list "and" %1 %2))))

(def Or
  "Parses the logical 'or' operator."
  (>> (token "or") (return #(list "or" %1 %2))))

(def StringValue
  "CIMI filter string values can be either single or double quoted values."
  (<|> string-lit sq-string-lit))

(def Value
  "Permitted values are dates, decimal integers, strings, and booleans."
  (<|> datetime-lit dec-lit StringValue bool-lit))

(def PropExpr
  "Parses expressions like: property['x'] != 3"
  (bind [_ (token "property")
         prop (brackets StringValue)
         op RestrictedOp
         value StringValue]
    (return (list op (list "get" "properties" prop) value))))

(def AttrExpr1
  "Parses expressions like: id = 4"
  (bind [attr identifier
         op Op
         value Value]
    (return (list op attr value))))

(def AttrExpr2
  "Parses expressions like: 4 = id"
  (bind [value Value
         op Op
         attr identifier]
    (return (list op value attr))))

(def ParenExpr
  "Filter inside of parentheses."
  (parens Filter))

(def Comp
  "Parses a single filter component."
  (<|> PropExpr AttrExpr1 AttrExpr2 ParenExpr))

(def AndExpr
  "Allows filter components to be combined with a logical 'and'."
  (chainl Comp And nil))

(def Filter
  "A filter consists of expressions separated by logical 'or'."
  (chainl AndExpr Or nil))
