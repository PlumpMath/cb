(defproject cb "0.1.0-SNAPSHOT"
  :description "Static Web site generator and test server"
  :url "https://github.com/eigenhombre/cb"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :dependencies [[com.stuartsierra/lazytest "1.2.3"]
                 [enlive "1.0.1"]
                 [hiccup "1.0.2"]
                 [fs "1.3.2"]
                 [markdown-clj "0.9.13"]
                 [midje "1.4.0"]
                 [org.clojure/clojure "1.3.0"]
                 [ring/ring-core "1.0.2"]
                 [ring/ring-jetty-adapter "1.0.2"]
                 [watchtower "0.1.1"]]
  :main cb.core
  :jar-name "cb.jar"
  :uberjar-name "cb-app.jar")
