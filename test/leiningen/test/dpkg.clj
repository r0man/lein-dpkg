(ns leiningen.test.dpkg
  (:refer-clojure :exclude (read replace))
  (:import java.io.File java.nio.file.Path)
  (:use [clojure.java.io :only (file)]
        [leiningen.core.project :only (read)]
        [leiningen.jar :only (get-jar-filename)]
        [leiningen.uberjar :only (uberjar)]        
        clojure.test
        leiningen.dpkg))

(def project (read))

(deftest test-build
  (doall (build project))
  (is (.exists (File. (deb-target-file project)))))

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

(deftest test-deb-target-uberjar
  (is (= (str (file (deb-java-dir project) "lein-dpkg.jar"))
         (deb-target-uberjar project))))

(deftest test-make-path
  (is (instance? Path (make-path "x"))))
