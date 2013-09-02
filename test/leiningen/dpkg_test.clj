(ns leiningen.dpkg-test
  (:refer-clojure :exclude [new read replace remove])
  (:import java.io.File java.nio.file.Path)
  (:use [clojure.java.io :only (file)]
        [leiningen.core.project :only (read)]
        [leiningen.jar :only (get-jar-filename)]
        [leiningen.uberjar :only (uberjar)]
        clojure.test
        leiningen.dpkg))

(def project (read))

(deftest test-parse-version
  (is (= {:major "0", :minor "1", :patch "2", :suffix "SNAPSHOT"}
         (parse-version "0.1.2-SNAPSHOT"))))

(deftest test-format-version
  (= "0.1.2-SNAPSHOT" (format-version {:major "0", :minor "1", :patch "2", :suffix "SNAPSHOT"}))
  (= "0.1.2-1123-SNAPSHOT" (format-version {:major "0", :minor "1", :patch "2", :incremental "1123" :suffix "SNAPSHOT"})))

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

(deftest test-deb-path
  (is (= (str (file (:target-path project) "debian" "usr" "lib" (:name project)))
         (deb-path project)))
  (is (= (str (file (:target-path project) "debian" "usr" "lib" (:name project) "lib"))
         (deb-path project "lib")))
  (is (= (str (file (:target-path project) "debian" "usr" "lib" (:name project) "src"))
         (deb-path project "src"))))

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
  (is (= (str (file (deb-path project "lib") "lein-dpkg.jar"))
         (deb-target-uberjar project))))

(deftest test-make-path
  (is (instance? Path (make-path "x"))))
