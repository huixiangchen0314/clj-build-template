(ns top.kzre.clj-build-template.tasks.delete
  (:require
    [clojure.string :as str]
    [clojure.tools.build.api :as api]
    [clojure.tools.build.tasks.copy :as copy])
  (:import
    [java.io File]
    [java.nio.file FileSystems FileVisitor FileVisitResult Files Path]))

(set! *warn-on-reflection* true)

(defn exts->include-glob
  "Convert a collection of file extensions (with or without a leading dot)
   into an include glob understood by copy-dir:
     [\"class\"]         -> \"**/*.class\"
     [\"clj\" \"cljc\"]  -> \"**/*.{clj,cljc}\""
  [exts]
  (let [exts (mapv #(str/replace % #"^\." "") exts)]
    (if (= 1 (count exts))
      (str "**/*." (first exts))
      (str "**/*.{" (str/join "," exts) "}"))))

(defn- match-paths
  "Match glob to paths under root and return a collection of Path objects."
  [^File root glob]
  (let [root-path (.toPath root)
        matcher   (.getPathMatcher (FileSystems/getDefault) (str "glob:" glob))
        paths     (volatile! [])
        visitor   (reify FileVisitor
                    (visitFile [_ path _attrs]
                      (when (.matches matcher (.relativize root-path ^Path path))
                        (vswap! paths conj path))
                      FileVisitResult/CONTINUE)
                    (visitFileFailed [_ _path _ex] FileVisitResult/CONTINUE)
                    (preVisitDirectory [_ _ _] FileVisitResult/CONTINUE)
                    (postVisitDirectory [_ _ _] FileVisitResult/CONTINUE))]
    (Files/walkFileTree root-path visitor)
    @paths))

(defn- delete-empty-dirs
  "Remove empty directories under `root` (bottom-up). The root itself is kept."
  [^Path root]
  (let [dirs    (volatile! [])
        visitor (reify FileVisitor
                  (visitFile [_ _ _] FileVisitResult/CONTINUE)
                  (visitFileFailed [_ _ _] FileVisitResult/CONTINUE)
                  (preVisitDirectory [_ dir _]
                    (vswap! dirs conj dir)
                    FileVisitResult/CONTINUE)
                  (postVisitDirectory [_ _ _] FileVisitResult/CONTINUE))]
    (Files/walkFileTree root visitor)
    (doseq [^Path dir (reverse @dirs)]
      (when (and (not (.equals dir root))
                 (with-open [s (Files/list dir)]
                   (not (.isPresent (.findFirst s)))))
        (Files/deleteIfExists dir)))))

(defn- target-root
  "Resolve :target-dir to an existing File (never creates it)."
  ^File [target-dir]
  (let [^File root (.toFile ^Path (api/resolve-path target-dir))]
    (when-not (.exists root)
      (throw (ex-info (str "target-dir does not exist: " target-dir)
                      {:target-dir target-dir})))
    root))

(defn- paths-to-delete
  "Resolve paths under target-dir matching include, minus ignored file names.
   Returns [root paths]."
  [target-dir include ignores]
  (let [root           (target-root target-dir)
        ignore-regexes (mapv re-pattern (or ignores copy/default-ignores))
        paths          (match-paths root include)]
    [root
     (into []
           (remove (fn [^Path p]
                     (copy/ignore? (.getName (.toFile p)) ignore-regexes)))
           paths)]))

(defn preview
  "Print the files that `delete` would remove (paths relative to :target-dir)
   and return the collection of `Path` objects. Does not touch the filesystem."
  [{:keys [target-dir include ignores backup-path]
    :or   {include "**"}
    :as   _params}]
  (let [[^File root paths] (paths-to-delete target-dir include ignores)
        root-path (.toPath root)]
    (println "Would delete" (count paths) "file(s) under" (.toString root-path) ":")
    (doseq [^Path p paths]
      (println " " (.toString (.relativize root-path p))))
    (when (and backup-path (seq paths))
      (println "Would back up to" (str backup-path)))
    paths))

(defn delete
  "Delete files under :target-dir matching :include glob.

   Parameters mirror `copy-dir` (:target-dir, :include, :ignores, backup opts),
   so backups are simply delegated to `copy-dir`.

   Options:
     :target-dir         - directory to delete from (required, must exist)
     :include            - glob matched against paths relative to :target-dir
                           (default \"**\")
     :ignores            - regexes matched against file *names* to skip
                           (defaults to `copy/default-ignores`, keeping behaviour
                           consistent with `copy-dir`)
     :backup-path        - optional; if given, matched files are first copied
                           to this directory using `copy-dir`
     :backup-overwrite?  - passed through to `copy-dir`; when false (default)
                           an existing backup entry is renamed instead of being
                           overwritten
     :delete-empty-dirs? - also prune directories that become empty
                           (default false)
     :dry-run?           - when true, just call `preview` and return the
                           would-be-deleted paths (default false)"
  [{:keys [target-dir include ignores backup-path backup-overwrite?
           delete-empty-dirs? dry-run?]
    :or   {include "**", backup-overwrite? false,
           delete-empty-dirs? false, dry-run? false}
    :as   params}]
  (if dry-run?
    (preview params)
    (let [[^File root paths] (paths-to-delete target-dir include ignores)]
      ;; 1) back up first — if this throws, nothing is deleted yet.
      ;;    Pass :ignores through so the backup set matches the delete set.
      (when (and backup-path (seq paths))
        (api/copy-dir {:src-dirs          [target-dir]
                        :target-dir        backup-path
                        :include           include
                        :ignores           ignores
                        :backup-overwrite? backup-overwrite?}))
      ;; 2) then delete
      (doseq [^Path p paths]
        (Files/deleteIfExists p))
      (when delete-empty-dirs?
        (delete-empty-dirs (.toPath root)))
      (println "Deleted" (count paths) "file(s) under" (.toString (.toPath root)))
      (count paths))))