(ns lein-dpkg.plugin)

(defn incremental? [project]
  (and (re-matches #".*-SNAPSHOT" (:version project))
       (not (nil? (-> project :dpkg :incremental)))))

(defn incremental-version [project]
  (if-let [env (-> project :dpkg :incremental)]
    (str (:version project) "-" (or (System/getenv env) "1"))))

(defn middleware [project]
  (if (incremental? project)
    (assoc project :version (incremental-version project))
    project))
