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
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [markdown.core :as md]
            [net.cgrand.enlive-html :as html]
            [watchtower.core :as wt]))


(defn target-file-name [filename sitedir]
  (let [[basename _] (path/splitext filename)]
    (path/join sitedir (str basename ".html"))))


(defn update-using-template [template html]
   (st/replace template
               #"(?i)(.*?<div id='_body'>)(</div>)"
               (format "$1%s$2" html)))


(defn engine [args]
  (let [files (path/files-in (:markupdir args))
        tf (get args :template nil)
        template (if tf
                   (slurp tf)
                   "<DIV ID='_body'></DIV>")]
    (fs/mkdir (:sitedir args))
    (doseq [f files]
      (let [filename (.getName f)
            target-name (target-file-name filename (:sitedir args))
            html-content (md/md-to-html-string (slurp f))
            updated (update-using-template template html-content)]
        (spit target-name updated)))))
        

(defn preprocess-html [s]
  (let [rdr (PushbackReader. (StringReader. s))
        edn (read rdr)
        rest (apply str (interpose "\n" (rest (line-seq (BufferedReader. rdr)))))]
    [edn rest]))


(def ^:dynamic *extensions-to-update* [:md :jpg :png :css])


(defn handle-update! [settings files]
  (println (format "Files changed (%s), triggering reprocessing (settings: %s)."
                   (->> files
                       (map #(.getPath %))
                       (interpose " ")
                       (apply str))
                   settings))
  ;; FIXME: why is a future needed here?
  ;; Otherwise updates don't work...
  (future (engine settings)))  



(defn watcher
  [root]
  (let [src (path/join root "markup")
        dest (path/join root "site")
        settings {:sitedir dest
                  :markupdir src}]
    (wt/watcher
     [src]
     (wt/rate 100)
     (wt/file-filter wt/ignore-dotfiles)
     (wt/file-filter (apply wt/extensions *extensions-to-update*))
     (wt/on-change (fn [files] (handle-update! settings files))))))


(defn server
  [root]
  (letfn [(serve-stat [req]
            (print "Serving" (:uri req) ": ")
            (let [r (resp/file-response (:uri req)
                                        {:root (path/join root "site")})]
              (println r)
              r))]
    (future (jetty/run-jetty serve-stat {:port 8080 :join? false}))))


(defn- do-main [toplevel]
  (server toplevel)
  (watcher toplevel))


(defn -main [& args]
  (let [[toplevel & more] args]
    (cond
     (nil? toplevel)   (println "Please supply a directory name"
                                "for your site's toplevel")
     (not (nil? more)) (println "Only one argument is required"
                                "(directory name for your site's toplevel)")
     :else (do-main toplevel))))
