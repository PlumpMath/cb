(ns cb.path-test
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


;; FIXME: DRY with core_test.clj
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

  
(facts "about splitext"
       (path/splitext "x.y")      => ["x" "y"]
       (path/splitext "x")        => ["x" ""]
       (path/splitext "/x/y/z.f") => ["/x/y/z" "f"]
       (path/splitext "/x/y/z")   => ["/x/y/z" ""]
       (path/splitext "/x/y.z/p.q/r.s.t") => ["/x/y.z/p.q/r.s" "t"])

(facts "about extension"
       (path/extension "")        => nil
       (path/extension "x")       => nil
       (path/extension "x.y")     => "y"
       (path/extension "z/x.y")   => "y"
       (path/extension "z.b/x.y") => "y"
       (path/extension "z.b/x")   => nil)


(facts "about pathjoin"
       (path/join "a") => "a"
       (path/join "/a") => "/a"
       (path/join "a" "b")  => "a/b"
       (path/join "a/" "b") => "a/b"
       (path/join "a" "/b") => "a/b"
       (path/join "/a" "b") => "/a/b"
       (path/join "a" "b" "c") => "a/b/c")


(fact "I can test for existence of a directory I know exists"
      (path/exists "/") => truthy)


(fact "I can create a test directory, verify its existence and its removal"
      (do (with-tmp-directory testdir
            (path/exists testdir) => truthy)
          (path/exists testdir) => falsey))


(fact "I can place a test file in a test directory, verify that file's
       existence, as well as its removal"
      (with-tmp-directory testdir
        (let [tf (path/join testdir "foo")]
          (with-tmp-file tf "contents"
            (path/exists tf) => truthy)
          (path/exists tf) => falsey)))


(fact "Putting a file in a test directory does not prevent deletion of the directory"
      (with-tmp-directory testdir
        (let [undeleted (path/join testdir "foo")]
          (spit undeleted "contents")))
      (path/exists testdir) => false)


(fact "with-setup creates markup source directory"
      (do
        (with-setup testdir
          (path/exists (path/join testdir "markup")) => truthy)
        (path/exists testdir) => falsey))
