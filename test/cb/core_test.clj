(ns cb.core-test
  (:import [java.io File])
  (:use clojure.test
        midje.sweet
        cb.core
        clojure.pprint)
  (:require [cb.path :as path]
            [fs.core :as fs]
            [markdown.core :as md]
            [net.cgrand.enlive-html :as html]))


(def testdir "/tmp/testdir_4_cb")


;; FIXME: DRY with path_test.clj
(defmacro with-tmp-directory [dirname & body]
  `(do
     ;;(assert (not (path/exists ~dirname)))
     (fs/mkdir ~dirname)
     ~@body
     (path/delete-file-recursively (File. ~dirname))))


(defmacro with-tmp-file [path body-content & body]
  `(do
     ;;(assert (not (path/exists ~path)))
     (spit ~path ~body-content)
     ~@body
     (fs/delete ~path)))


(defmacro with-setup [dir & body]
  `(with-tmp-directory ~dir
     (let [markup# (path/join ~dir "markup")]
       (with-tmp-directory markup#
         ~@body))))


(fact "Trivial Markdown transformation works"
      (md/md-to-html-string "hi") => "<p>hi</p>")


(fact "with-setup creates markup source directory"
      (do
        (with-setup testdir
          (path/exists (path/join testdir "markup")) => truthy)
        (path/exists testdir) => falsey))


(def markupdir (path/join testdir   "markup"))
(def sitedir   (path/join testdir   "site"))
(def indexmd   (path/join markupdir "index.md"))
(def indexhtml (path/join sitedir   "index.html"))
(def template  (path/join markupdir "template.html"))


(fact
 "engine processes markup/index.md into site/index.html"
 (with-setup testdir
   (with-tmp-file indexmd "hello"
     (engine {:sitedir sitedir, :markupdir markupdir})
     (path/exists indexhtml) => truthy)))


(fact
 "interpolation of HTML content into template works"
 (let []
   (with-setup testdir
     (with-tmp-file indexmd "hello"
       (with-tmp-file template "FindMe <DIV ID='_body'></DIV>"
         (engine {:sitedir sitedir
                  :markupdir markupdir
                  :template template})
         (let [result (slurp indexhtml)]
           result => (contains "FindMe")
           result => (contains "hello")))))))


(fact
 "interpolation of HTML content into template,
  with default template string, works"
 (let []
   (with-setup testdir
     (with-tmp-file indexmd "hello"
       (engine {:sitedir sitedir
                :markupdir markupdir})
       (let [result (slurp indexhtml)]
         result => (contains "hello"))))))


(def ex1 "{:a :map
           :with 3
           :things nil}
<HEAD>
</HEAD>
<BODY>Mmmmm..... bodies....</BODY>")


(fact "strings can be broken into EDN and HTML portions"
      (let [[edn html] (preprocess-html ex1)]
        (class edn) => (class {})
        (class html) => (class "")
        (:a edn) => :map
        html => (contains "/HEAD")))
