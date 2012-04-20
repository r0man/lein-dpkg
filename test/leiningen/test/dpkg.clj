(ns leiningen.test.dpkg
  (:refer-clojure :exclude (read replace))
  (:import java.io.File java.nio.file.Path)
  (:require [leiningen.jar :as jar])
  (:use [clojure.java.io :only (file)]
        [leiningen.core.project :only (read)]
        [leiningen.uberjar :only (uberjar)]        
        clojure.test
        leiningen.dpkg))

(def project (read))

(deftest test-build
  (build project)
  (is (not (.exists (File. (deb-target-file project))))))

(deftest test-clean
  (clean project)
  (is (not (.exists (File. (deb-target-dir project))))))

(deftest test-copy-uberjar
  (uberjar project)
  (copy-uberjar project)
  (is (.exists (File. (deb-target-uberjar project)))))

(deftest test-deb-java-dir
  (is (= (str (file (:target-path project) "debian" "usr" "share" "java"))
         (deb-java-dir project)))
  (is (= (str (file (:target-path project) "debian" "usr" "local" "share" "java"))
         (deb-java-dir (assoc-in project [:dpkg :java-dir] (str (file "/" "usr" "local" "share" "java")))))))

(deftest test-deb-source-dir
  (is (= (str (file (:root project) "debian"))
         (deb-source-dir project))))

(deftest test-deb-target-dir
  (is (= (str (file (:target-path project) "debian"))
         (deb-target-dir project))))

(deftest test-deb-target-file
  (is (= (str (file (:target-path project) (format "lein-dpkg-%s.deb" (:version project))))
         (deb-target-file project))))

(deftest test-deb-target-path
  (is (= (str (file (deb-target-dir project) "x"))
         (deb-target-path project (file (deb-source-dir project) "x")))))

(deftest test-deb-target-symlink
  (is (= (str (file (deb-java-dir project) "lein-dpkg.jar"))
         (deb-target-symlink project))))

(deftest test-deb-target-uberjar
  (is (= (str (file (deb-java-dir project) (.getName (File. (jar/get-jar-filename project :uberjar)))))
         (deb-target-uberjar project))))

(deftest test-make-path
  (is (instance? Path (make-path "x"))))

(deftest test-symlink-uberjar
  (uberjar project)
  (copy-uberjar project)
  (symlink-uberjar project)
  (is (.exists (File. (deb-target-symlink project)))))
