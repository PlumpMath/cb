(ns cb.core-test
  (:import [java.io
            File
            PushbackReader
            StringReader
            BufferedReader])
  (:use clojure.test
        midje.sweet
        cb.core
        clojure.pprint)
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [fs.core :as fs]
            [markdown.core :as md]
            [net.cgrand.enlive-html :as html]
            [watchtower.core :as w]))


(def testdir "/tmp/testdir_4_cb")


(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its
contents. Raise an exception if any deletion fails unless silently is
true. [stolen/modified from clojure-contrib]"
  [f]
  (if (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-file-recursively child)))
  (.delete f))

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

(defn pathjoin [& args]
  "Join any sequence of directory names, correctly handling duplicate
'/' characters, including leading '/'."
  (let [base (st/join "/"
                      (map (comp #(st/replace % #"/$" "")
                                 #(st/replace % #"^/" ""))
                           args))]
    (if (= (first (first args)) \/)
      (str "/" base)
      base)))

(defn path-exists [path] (-> path File. .exists))

(defn files-in [dirname]
  (-> dirname
      File.
      .listFiles
      seq))

(comment
  
(defn new-filenames-and-extensions
  [files rootdir]
  (map (fn [F]
         (let [path (.getPath F)
               path-in-markup-dir (st/replace path (pathjoin rootdir "markup/") "")
               ext (extension path)
               newext (dest-file-extension ext)
               newfile-oldext (pathjoin rootdir "site" path-in-markup-dir)
               [newf _] (splitext newfile-oldext)
               newfile (str newf "." newext)]
           [path newfile newext]))
       files))

)

(defn update-file-path-and-ext [filename sitedir]
  (let [[basename _] (splitext filename)]
    (pathjoin sitedir (str basename ".html"))))

(defn engine [args]
  (let [files (files-in (:markupdir args))]
    (fs/mkdir (:sitedir args))
    (doseq [f files]
      (let [filename (.getName f)
            target-name (update-file-path-and-ext filename (:sitedir args))
            markdown-content (slurp f)
            html-content (md/md-to-html-string markdown-content)]
        (spit target-name html-content)))))

(defn remove-files [coll]
  (map #(.delete %) coll))

(defmacro with-tmp-directory [dirname & body]
  `(do
     ;;(assert (not (path-exists ~dirname)))
     (fs/mkdir ~dirname)
     ~@body
     (delete-file-recursively (File. ~dirname))))

(defmacro with-tmp-file [path body-content & body]
  `(do
     ;;(assert (not (path-exists ~path)))
     (spit ~path ~body-content)
     ~@body
     (fs/delete ~path)))

(defmacro with-setup [dir & body]
  `(with-tmp-directory ~dir
     (let [markup# (pathjoin ~dir "markup")]
       (with-tmp-directory markup#
         ~@body))))

(try
  
(facts "about splitext"
       (splitext "x.y")      => ["x" "y"]
       (splitext "x")        => ["x" ""]
       (splitext "/x/y/z.f") => ["/x/y/z" "f"]
       (splitext "/x/y/z")   => ["/x/y/z" ""]
       (splitext "/x/y.z/p.q/r.s.t") => ["/x/y.z/p.q/r.s" "t"])

(facts "about extension"
       (extension "")        => nil
       (extension "x")       => nil
       (extension "x.y")     => "y"
       (extension "z/x.y")   => "y"
       (extension "z.b/x.y") => "y"
       (extension "z.b/x")   => nil)


(facts "about pathjoin"
       (pathjoin "a") => "a"
       (pathjoin "/a") => "/a"
       (pathjoin "a" "b")  => "a/b"
       (pathjoin "a/" "b") => "a/b"
       (pathjoin "a" "/b") => "a/b"
       (pathjoin "/a" "b") => "/a/b"
       (pathjoin "a" "b" "c") => "a/b/c")

(fact "I can test for existence of a directory I know exists"
      (path-exists "/") => truthy)

(fact "I can create a test directory, verify its existence and its removal"
      (do (with-tmp-directory testdir
            (path-exists testdir) => truthy)
          (path-exists testdir) => falsey))

(fact "I can place a test file in a test directory, verify that file's
       existence, as well as its removal"
      (with-tmp-directory testdir
        (let [tf (pathjoin testdir "foo")]
          (with-tmp-file tf "contents"
            (path-exists tf) => truthy)
          (path-exists tf) => falsey)))

(fact "Putting a file in a test directory does not prevent deletion of the directory"
      (with-tmp-directory testdir
        (let [undeleted (pathjoin testdir "foo")]
          (spit undeleted "contents")))
      (path-exists testdir) => false)

(fact "Trivial Markdown transformation works"
      (md/md-to-html-string "hi") => "<p>hi</p>")

(fact "with-setup creates markup source directory"
      (do
        (with-setup testdir
          (path-exists (pathjoin testdir "markup")) => truthy)
        (path-exists testdir) => falsey))

(fact "engine processes markup/index.md into site/index.html"
      (let [markupdir (pathjoin testdir "markup")
            sitedir (pathjoin testdir "site")
            testfile (pathjoin markupdir "index.md")
            outfile (pathjoin sitedir "index.html")]
        (with-setup testdir
          (with-tmp-file testfile "hello"
            (engine {:sitedir sitedir, :markupdir markupdir})
            (path-exists outfile) => truthy))))

(def ex1 "{:a :map
           :with 3
           :things nil}
<HEAD>
</HEAD>
<BODY>Mmmmm..... bodies....</BODY>")

(defn preprocess-html [s]
  (let [rdr (PushbackReader. (StringReader. s))
        edn (read rdr)
        rest (apply str (interpose "\n" (rest (line-seq (BufferedReader. rdr)))))]
    [edn rest]))

(fact "strings can be broken into EDN and HTML portions"
      (let [[edn html] (preprocess-html ex1)]
        (class edn) => (class {})
        (class html) => (class "")
        (:a edn) => :map
        html => (contains "/HEAD")))

;; NEXT: process all markdown files and make HTML files in target directory
(catch Exception e (prn e)))
