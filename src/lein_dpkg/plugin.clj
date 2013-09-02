(ns lein-dpkg.plugin
  (:require [clojure.string :refer [blank?]]
            [leiningen.dpkg :as dpkg]))

(defn incremental? [project]
  (and (re-matches #".*-SNAPSHOT" (:version project))
       (not (nil? (-> project :dpkg :incremental)))))

(defn incremental-version [project]
  (let [env (or (-> project :dpkg :incremental) "BUILD_NUMBER")
        incremental (System/getenv env)]
    (if-not (blank? incremental)
      (-> (:version project)
          (dpkg/parse-version)
          (assoc :incremental incremental)
          (dpkg/format-version))
      (:version project))))

(defn middleware [project]
  (if (incremental? project)
    (assoc project :version (incremental-version project))
    project))
