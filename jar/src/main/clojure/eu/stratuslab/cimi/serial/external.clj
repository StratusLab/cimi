(ns eu.stratuslab.cimi.serial.external
  "Serialize clojure representations of CIMI resources into external
  representations, namely JSON and XML."
  (:require [clojure.string :as str])
  (:import [javax.xml.stream XMLOutputFactory XMLStreamWriter]
           [java.io StringWriter]
           [java.net URI]))

(def ^:const cimi-namespace "http://schemas.dmtf.org/cimi/1")

(defn resource-name
  [uri]
  (if uri
    (let [path (.getPath (URI. uri))]
      (last (str/split path #"/")))))

(defn xml-output-factory
  ^XMLOutputFactory []
  (doto (XMLOutputFactory/newFactory)
    (.setProperty "javax.xml.stream.isRepairingNamespaces" Boolean/TRUE)))

(defmulti serialize-element
          (fn [xml-writer key value]
            key)
          :default nil)

(defmethod serialize-element nil
           [^XMLStreamWriter xml-writer key value]
  (doto xml-writer
    (.writeStartElement key)
    (.writeCharacters (str value))
    (.writeEndElement)))

(defmethod serialize-element "resourceURI"
           [xml-writer key value]
  nil)

(defmethod serialize-element "properties"
           [^XMLStreamWriter xml-writer key value]
  (doall
    (for [[k v] value]
      (doto xml-writer
        (.writeStartElement "property")
        (.writeAttribute "key" k)
        (.writeCharacters (str v))
        (.writeEndElement)))))

(defn serialize-elements
  [xml-writer data]
  (doall (map (fn [[k v]] (serialize-element xml-writer (name k) v)) data)))

(defn resource-as-xml
  [{:keys [resourceURI] :as data}]
  (let [factory (xml-output-factory)]
    (with-open [writer (StringWriter.)]
      (let [^XMLStreamWriter xml-writer (.createXMLStreamWriter factory writer)
            root-element-name (resource-name resourceURI)]
        (doto xml-writer
          (.writeStartDocument)
          (.setDefaultNamespace cimi-namespace)
          (.writeStartElement cimi-namespace root-element-name)
          (serialize-elements data)
          (.writeEndElement)
          (.writeEndDocument))
        (.toString writer)))))
