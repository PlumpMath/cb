(ns cb.path
    (:import [java.io
              File
              PushbackReader
              StringReader
              BufferedReader])
    (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [fs.core :as fs]
            [markdown.core :as md]
            [net.cgrand.enlive-html :as html]
            [watchtower.core :as w]))


(defn exists [path] (-> path File. .exists))


(defn splitext [path]
  (let [parts (.split path "\\.")]
    (if (= 1 (count parts))
      [path ""]
      [(st/join "." (drop-last parts))
       (last parts)])))


(defn extension [path]
  (let [no-dir (last (.split path "\\/"))]
    (if (.contains no-dir ".")
      (last (.split no-dir "\\.")))))


(defn join [& args]
  "Join any sequence of directory names, correctly handling duplicate
'/' characters, including leading '/'."
  (let [base (st/join "/"
                      (map (comp #(st/replace % #"/$" "")
                                 #(st/replace % #"^/" ""))
                           args))]
    (if (= (first (first args)) \/)
      (str "/" base)
      base)))



(defn files-in [dirname]
  (-> dirname
      File.
      .listFiles
      seq))


(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its
contents. Raise an exception if any deletion fails unless silently is
true. [stolen/modified from clojure-contrib]"
  [f]
  (if (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-file-recursively child)))
  (.delete f))
