(ns run-codox
  (:require [codox.main :as codox]
            [codox.writer.html :refer :all]))

(def args
     {:group "${project.groupId}",
      :output-dir "target/codox",
      :name "${project.artifactId}",
      :sources ["src/main/clojure"],
      :description "${project.name}"
      :writer 'codox.writer.html/write-docs})

(codox/generate-docs args)



