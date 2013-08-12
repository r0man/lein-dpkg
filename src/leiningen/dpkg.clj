(ns leiningen.dpkg
  (:refer-clojure :exclude [new read replace remove])
  (:import [java.nio.file Files LinkOption Paths]
           java.io.File)
  (:require [clojure.java.io :refer [copy file]]
            [clojure.java.shell :refer [sh with-sh-dir]]
            [clojure.string :refer [blank? replace]]
            [leiningen.clean :refer [delete-file-recursively]]
            [leiningen.jar :refer [get-jar-filename]]
            [leiningen.new.templates :refer [render-text]]
            [leiningen.uberjar :refer [uberjar]]))

(defn make-path
  "Convert `path` to a java.nio.file.Path object."
  [path] (Paths/get (str path) (into-array String [])))

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
  [project] (replace (get-jar-filename project) #"jar$" "deb"))

(defn deb-target-uberjar
  "Returns the filename of the uberjar in the target debian directory."
  [project]
  (str (file (deb-java-dir project) (str (replace (:name project) ".*/" "") ".jar"))))

(defn sh! [cmd & args]
  (let [result (apply sh cmd args)]
    (when-not (blank? (:err result))
      (print (:err result))
      (flush))
    (if-not (= (:exit result) 0)
      (throw (Exception. (:err result))))
    result))

(defn build-package [project]
  (with-sh-dir (:root project)
    (let [script (str (:root project) "/.lein-dpkg")]
      (when (.exists (file script))
        (print (:out (sh! script)))
        (flush)))
    (sh! "fakeroot" "dpkg-deb" "--build" (deb-target-dir project))))

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
          :let [target (deb-target-path project source)
                permissions (Files/getPosixFilePermissions (make-path source) (into-array LinkOption []))]]
    (.mkdirs (.getParentFile (file target)))
    (spit target (render-text (slurp source) project))
    (Files/setPosixFilePermissions (make-path target) permissions)))

(defn copy-uberjar
  "Copy the uberjar to the debian target directory."
  [project]
  (.mkdirs (file (deb-java-dir project)))
  (copy (file (get-jar-filename project :uberjar))
      (file (deb-target-uberjar project))))

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
    (render-templates)
    (build-package)
    (rename-package)))

(defn install
  "Install the Debian package."
  [project]
  (if-not (.exists (file (deb-target-file project)))
    (build project))
  (sh! "sudo" "dpkg" "--install" (deb-target-file project)))

(defn remove
  "Remove the Debian package."
  [project] (sh! "sudo" "dpkg" "--remove" (:name project)))

(defn purge
  "Purge the Debian package."
  [project] (sh! "sudo" "dpkg" "--purge" (:name project)))

(defn dpkg
  "Build the Debian package."
  [project & [command]]
  (case command
    "build" (build project)
    "clean" (clean project)
    "install" (install project)
    "purge" (purge project)
    "remove" (remove project)
    (build project)))
