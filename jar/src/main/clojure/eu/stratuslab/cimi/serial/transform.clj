(ns eu.stratuslab.cimi.serial.transform
  (:require
    [clojure.java.io :as io])
  (:import
    [javax.xml.transform Transformer TransformerFactory]
    [javax.xml.transform.dom DOMSource]
    [javax.xml.transform.stream StreamSource StreamResult]
    [javax.xml.parsers DocumentBuilder DocumentBuilderFactory]
    [java.io StringReader StringWriter ByteArrayInputStream]))

(def strip-transform
  "<xsl:stylesheet version='1.0'
                   xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                   xmlns:cimi='http://cimi'>

    <xsl:template match='comment()|processing-instruction()' />

    <xsl:template match='*'>
      <xsl:if test='namespace-uri()=\"http://cimi\"'>
        <!-- remove element prefix -->
        <xsl:element name='{local-name()}'>
          <!-- process attributes -->
          <xsl:for-each select='@*'>
            <!-- remove attribute prefix -->
            <xsl:attribute name='{local-name()}'>
              <xsl:value-of select='.'/>
            </xsl:attribute>
          </xsl:for-each>
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:if>
    </xsl:template>

  </xsl:stylesheet>")

(def ^DocumentBuilderFactory document-builder-factory
  (doto (DocumentBuilderFactory/newInstance)
    (.setNamespaceAware true)
    (.setValidating false)))

(defn get-transformer
  ^Transformer []
  (->> strip-transform
       (StringReader.)
       (StreamSource.)
       (.newTransformer (TransformerFactory/newInstance))))

(defn get-doc-builder
  ^DocumentBuilder []
  (.newDocumentBuilder document-builder-factory))

(defn get-doc-source
  ^DOMSource [^String input]
  (->> (.getBytes input "UTF-8")
       (ByteArrayInputStream.)
       (.parse (get-doc-builder))
       (DOMSource.)))

(defn strip-non-cimi-elements [input]
  (let [source (get-doc-source input)
        transformer (get-transformer)
        result (StringWriter.)
        resultStream (StreamResult. result)]
    (.transform transformer source resultStream)
    (.toString result)))
