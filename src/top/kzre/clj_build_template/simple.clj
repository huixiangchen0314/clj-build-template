(ns top.kzre.clj-build-template.simple
  "纯clojure项目"
  (:require
    [clojure.string :as str]
    [clojure.tools.build.api :as b]
    [clojure.tools.build.tasks.copy :as copy]
    [top.kzre.clj-build-template.template :as template]))

(defn create-template []
  ((requiring-resolve 'top.kzre.clj-build-template.template/create-template)))

(defn clean []
  (b/delete {:path (template/dist-dir)}))

(defn- copy-src
  [{:keys [template src-dirs src-exts]
    :as   _params}]
  (let [common {:src-dirs   (or src-dirs (:src-dirs template))
                :target-dir (template/class-dir)}]
    (if (nil? src-exts)
      ;; src-exts 为 nil -> 复制所有文件，只排除 .java
      (b/copy-dir (assoc common
                    :ignores (conj copy/default-ignores #".*\.java$")))
      ;; src-exts 有值 -> 白名单，只复制这些后缀
      (let [exts (mapv #(str/replace % #"^\." "") src-exts)
            glob (if (= 1 (count exts))
                   (str "**/*." (first exts))
                   (str "**/*.{" (str/join "," exts) "}"))]
        (b/copy-dir (assoc common :include glob))))))

(defn write-pom [{:keys [template artifact version lisences]
                  :as _params}]
  (b/write-pom
    (cond->
      (template/pom-params template artifact version)
      lisences (assoc-in [:pom-data :licenses] lisences))))

(defn compile-java [{:keys [template src-dirs]}]
  (let [{:keys [java-version]} template]
    (b/javac {:src-dirs (or src-dirs (:src-dirs template))
              :class-dir (template/class-dir)
              :basis  (template/basis)
              :javac-opts ["-source" java-version
                           "-target" java-version]})))

(defn jar [{:keys
            [template artifact version
             compile-java?]
            :as params}]
  (clean)
  (copy-src params)
  (write-pom params)
  (when compile-java? (compile-java params))
  (b/jar {:class-dir (template/class-dir)
          :jar-file  (template/jar-file template artifact version)}))

(defn install [{:keys [template artifact version]
                :as params}]
  (jar params)
  (b/install {:basis    (template/basis)
              :lib       (template/lib template artifact )
              :version   version
              :jar-file  (template/jar-file template artifact version)
              :class-dir (template/class-dir)}))