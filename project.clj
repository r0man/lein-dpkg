(defproject lein-dpkg "0.1.2-SNAPSHOT"
  :description "Leiningen plugin for the Debian package management system"
  :url "https://github.com/r0man/lein-dpkg"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]]
  :eval-in-leiningen true
  :dpkg {:incremental "BUILD_NUMBER"}
  ;; :plugins [[lein-dpkg "0.1.2-SNAPSHOT"]]
  )
