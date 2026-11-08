(ns top.kzre.clj-build-template.api
  "任务构建apu"
  (:require [clojure.set :as set]))

(defn- assert-required
  "Check that each key in required coll is a key in params and throw if
  required are missing in params, otherwise return nil."
  [task params required]
  (let [missing (set/difference (set required) (set (keys params)))]
    (when (seq missing)
      (throw (ex-info (format "Missing required params for %s: %s" task (vec (sort missing))) (or params {}))))))

(defn delete-dir
  "文件夹删除，配置和 copy-dir 类似"
  [params]
  (assert-required "delete" params [:target-dir])
  ((requiring-resolve 'top.kzre.clj-build-template.tasks.delete/delete) params))

(defn exts->include-glob
  [exts]
  ((requiring-resolve 'top.kzre.clj-build-template.tasks.delete/exts->include-glob) exts))