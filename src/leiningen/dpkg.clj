(ns leiningen.dpkg
  (:refer-clojure :exclude [new read replace remove])
  (:import [java.nio.file Files LinkOption Paths]
           [org.apache.commons.io FileUtils]
           java.io.File)
  (:require [clojure.java.io :refer [copy file]]
            [leiningen.ring.uberjar :as ruberjar]
            [clojure.java.shell :refer [sh with-sh-dir]]
            [clojure.string :refer [blank? join replace]]
            [leiningen.clean :refer [delete-file-recursively]]
            [leiningen.jar :refer [get-jar-filename]]
            [leiningen.new.templates :refer [render-text]]
            [leiningen.uberjar :refer [uberjar]]))

(defn parse-version [s]
  (if-let [m (re-matches #"(\d+)\.(\d+)\.(\d+)(-(SNAPSHOT))?" (str s))]
    {:major (nth m 1)
     :minor (nth m 2)
     :patch (nth m 3)
     :suffix (nth m 5)}))

(defn format-version [version]
  (let [{:keys [major minor patch incremental suffix]} version]
    (str (join "." (clojure.core/remove blank? [major minor patch]))
         (if incremental (str "-" incremental))
         (if suffix (str "-" suffix)))))

(defn make-path
  "Convert `path` to a java.nio.file.Path object."
  [path] (Paths/get (str path) (into-array String [])))

(defn deb-path
  "Returns the project directory on Debian systems."
  [project & args]
  (->> (replace (str (apply file "usr" "lib" (:name project) args))
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

(defn deb-target-changes-file
  "Returns the filename of the changes file in the target directory."
  [project] (replace (get-jar-filename project) #"jar$" "changes"))

(def deb-changes-template "debian/DEBIAN/template.changes")

(defn deb-target-uberjar
  "Returns the filename of the uberjar in the target debian directory."
  [project]
  (str (file (deb-path project "lib") (str (replace (:name project) ".*/" "") ".jar"))))

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
    (FileUtils/copyFile
     (file (:root project) "project.clj")
     (file (deb-path project "project.clj")))
    (doseq [dir (map file (concat (:source-paths project) (:resource-paths project))) :when (.exists dir)]
      (FileUtils/copyDirectory dir (file (deb-path project (.getName dir)))))
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

(defn file-md5
  [f]
  (let [fis (new java.io.FileInputStream (file f))
        md5 (org.apache.commons.codec.digest.DigestUtils/md5Hex fis)]
        (.close fis)
        md5))

(defn render-changes
  "Render templates and write them to the target directory."
  [project]
  (assert (.exists (file deb-changes-template)))
  (let [debianPath (deb-target-file project)
        outputFile (deb-target-changes-file project) 
        size (.length (file debianPath))
        md5 (file-md5 debianPath)
        date (.toString (org.joda.time.DateTime/now) (org.joda.time.format.ISODateTimeFormat/dateTime))
        deb-file (.getName (file debianPath))
        project (merge project {:size size :md5 md5 :date date :deb-file deb-file})]
    (spit outputFile (render-text (slurp deb-changes-template) project))))

(defn copy-uberjar
  "Copy the uberjar to the debian target directory."
  [project]
  (.mkdirs (file (deb-path project "lib")))
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
    (rename-package)
    (render-changes)))

(defn build-ring
  "Build the Debian package for a ring project."
  [project]
  (doto project
    (clean)
    (ruberjar/uberjar)
    (copy-uberjar)
    (render-templates)
    (build-package)
    (rename-package)
    (render-changes)
    ))

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
    "build-ring" (build-ring project)
    "clean" (clean project)
    "install" (install project)
    "purge" (purge project)
    "remove" (remove project)
    (build project)))
