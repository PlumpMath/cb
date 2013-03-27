(ns cb.core
  (:gen-class)
  (:import [java.io
            File
            PushbackReader
            StringReader
            BufferedReader])
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [cb.path :as path]
            [fs.core :as fs]
            [markdown.core :as md]
            [net.cgrand.enlive-html :as html]
            [watchtower.core :as w]))


(defn update-file-path-and-ext [filename sitedir]
  (let [[basename _] (path/splitext filename)]
    (path/join sitedir (str basename ".html"))))


(defn engine [args]
  (let [files (path/files-in (:markupdir args))
        tf (get args :template nil)
        template (if tf
                   (slurp tf)
                   "<DIV ID='_body'></DIV>")]
    (fs/mkdir (:sitedir args))
    (doseq [f files]
      (let [filename (.getName f)
            target-name (update-file-path-and-ext filename (:sitedir args))
            markdown-content (slurp f)
            html-content (md/md-to-html-string markdown-content)
            updated (st/replace template
                                #"(?i)(.*?<div id='_body'>)(</div>)"
                                (format "$1%s$2" html-content))]
        (spit target-name updated)))))
        


(defn preprocess-html [s]
  (let [rdr (PushbackReader. (StringReader. s))
        edn (read rdr)
        rest (apply str (interpose "\n" (rest (line-seq (BufferedReader. rdr)))))]
    [edn rest]))


(defn -main []
  (println "OK"))
