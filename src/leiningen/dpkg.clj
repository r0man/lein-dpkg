(ns leiningen.dpkg
  (:refer-clojure :exclude (new read replace))
  (:require [leiningen.jar :as jar])
  (:import [java.nio.file Files Paths]
           java.nio.file.attribute.FileAttribute
           java.io.File)
  (:use [clojure.java.io :only (copy file)]
        [clojure.java.shell :only (sh with-sh-dir)]
        [clojure.string :only (blank? replace)]
        [leiningen.clean :only (delete-file-recursively)]
        [leiningen.core.project :only (read)]
        [leiningen.new.templates :only (render-text)]
        [leiningen.uberjar :only (uberjar)]))

(defn make-path
  "Convert `path` to a java.nio.file.Path object."
  [path] (Paths/get path (into-array String [])))

(defn symlink
  "Create a symbolic link from `source` to `target`."
  [source target]
  (Files/createSymbolicLink
   (make-path target)
   (make-path source)
   (into-array FileAttribute [])))

(defn deb-java-dir
  "Returns the Java directory on Debian systems."
  [project]
  (->> (replace (or (:java-dir (:dpkg project))
                    (str (file "usr" "share" "java")))
                (re-pattern (str "^" File/separator)) "")       
       (file (:target-path project) "debian") str))

(defn deb-source-dir
  "Returns the \"debian\" source directory of `project`."
  [project] (str (file (:root project) "debian")))

(defn deb-target-dir
  "Returns the \"debian\" target directory of `project`."
  [project] (str (file (:target-path project) "debian")))

(defn deb-target-path
  "Returns the path name of `source` in the \"debian\" target directory of `project`."
  [project source]
  (replace (str source) (deb-source-dir project) (deb-target-dir project)))

(defn deb-target-file
  "Returns the filename of the debian package the target directory."
  [project] (replace (jar/get-jar-filename project) #"jar$" "deb"))

(defn deb-target-symlink
  "Returns the filename of the uberjar in the target \"debian\" directory."
  [project]
  (str (file (deb-java-dir project) (str (replace (:name project) ".*/" "") ".jar"))))

(defn deb-target-uberjar
  "Returns the filename of the uberjar in the target debian directory."
  [project]  
  (str (file (deb-java-dir project) (.getName (File. (jar/get-jar-filename project :uberjar))))))

(defn build-package [project]
  (with-sh-dir (:root project)
    (let [result (sh "dpkg-deb" "--build" (deb-target-dir project))]
      (if-not (blank? (:err result))
        (println (:err result)))
      (if-not (= (:exit result) 0)
        (throw (Exception. (:err result)))))))

(defn rename-package
  "Rename the \"debian.deb\" file in the target directory to the same
  name as the jar file, but with a \"deb\" extension. "
  [project]
  (let [target (file (deb-target-file project))]
    (.renameTo (file (:target-path project) "debian.deb") target)
    (println (str "Created " target))))

(defn render-templates
  "Render templates and write them to the target directory."
  [project]
  (doseq [source (file-seq (file (:root project) "debian"))
          :when (.isFile source)
          :let [target (deb-target-path project source)]]
    (.mkdirs (.getParentFile (file target)))
    (spit target (render-text (slurp source) project))))

(defn copy-uberjar
  "Copy the uberjar to the debian target directory."
  [project]
  (.mkdirs (file (deb-java-dir project)))
  (copy (file (jar/get-jar-filename project :uberjar))
        (file (deb-target-uberjar project))))

(defn symlink-uberjar
  "Symlink the versioned uberjar in the debian target directory."
  [project]
  (let [target (deb-target-symlink project)]
    (.delete (File. target))
    (symlink (deb-target-uberjar project) target)))

(defn clean
  "Remove the \"debian\" directory from the project's target-path."
  [project] (delete-file-recursively (deb-target-dir project) true))

(defn build
  "Build the Debian package."
  [project]
  (doto project
    (clean)
    (uberjar)
    (copy-uberjar)
    (symlink-uberjar)
    (render-templates)
    (build-package)
    (rename-package)))

(defn dpkg
  "Build the Debian package."
  [project & [command]]
  (condp = command
    "build" (build project)
    "clean" (clean project)
    (build project)))
