(ns cb.core-test
  (:import java.io.File)           
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

(defn engine [dir]
  (fs/mkdir (pathjoin dir "site")))

(defn files-in [dirname]
  (-> dirname
      File.
      .listFiles
      seq))

(defn remove-files [coll]
  (println coll) 
  (map fs/delete coll))

(defmacro with-directory [dirname & body]
  `(do
     (assert (not (path-exists ~dirname)))
     (fs/mkdir ~dirname)
     ~@body
     (remove-files (files-in ~dirname))
     (fs/delete ~dirname)))

(defmacro with-file [path body-content & body]
  `(do
     (assert (not (path-exists ~path)))
     (spit ~path ~body-content)
     ~@body
     (fs/delete ~path)))

(defmacro with-setup [dir & body]
  `(with-directory ~dir
     (let [markup# (pathjoin ~dir "markup")]
       (with-directory markup#
         ~@body))))

(defmacro with-engine [dir & body]
  `(do
     (engine ~dir)
     ~@body))

(defn path-exists [path] (-> path File. .exists))

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
      (do (with-directory testdir
            (path-exists testdir) => truthy)
          (path-exists testdir) => falsey))

(fact "I can place a file in a test directory and verify that file's existence"
      (with-directory testdir
        (let [tf (pathjoin testdir "foo")]
          (with-file tf "contents"
            (path-exists tf) => truthy))))

(fact "Trivial Markdown transformation works"
      (md/md-to-html-string "hi") => "<p>hi</p>")

(fact "with-setup creates markup source directory"
      (do
        (with-setup testdir
          (path-exists (pathjoin testdir "markup")) => truthy)
        (path-exists testdir) => falsey))

;; (fact "running main engine creates target / site directory"
;;       (with-setup testdir
;;         (with-engine testdir
;;           (path-exists (pathjoin testdir "site")) => truthy)))

