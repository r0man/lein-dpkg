(defproject lein-dpkg "0.1.3"
  :description "Leiningen plugin for the Debian package management system"
  :url "https://github.com/r0man/lein-dpkg"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.commons/commons-io "1.3.2"]]
  :deploy-repositories [["releases" :clojars]]
  :eval-in-leiningen true
  :dpkg {:incremental "BUILD_NUMBER"}
  :min-lein-version "2.0.0")
