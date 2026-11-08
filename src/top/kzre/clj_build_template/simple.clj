(ns top.kzre.clj-build-template.simple
  "简单编译模板，不加* 是单步骤，加 * 是从clean开始的完整步骤"
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
  [{:keys [template
           src-dirs src-exts]
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

(defn- copy-resources
  [{:keys [resource-dirs]
    :as   _params}]
  (when (seq resource-dirs)
    (b/copy-dir {:src-dirs resource-dirs
                 :target-dir (template/class-dir)})))

(defn- write-pom [{:keys [template artifact version lisences]
                  :as _params}]
  (b/write-pom
    (cond->
      (template/pom-params template artifact version)
      lisences (assoc-in [:pom-data :licenses] lisences))))

(defn compile-java [{:keys [template src-dirs java-version]}]
  (let [java-version (or java-version (:java-version template))
        src-dirs (or src-dirs (:src-dirs template))]
    (b/javac {:src-dirs src-dirs
              :class-dir (template/class-dir)
              :basis  (template/basis)
              :javac-opts ["-source" java-version
                           "-target" java-version]})))

(defn compile-clj [{:keys [template src-dirs]}]
  (let [src-dirs (or src-dirs (:src-dirs template))]
    (b/compile-clj {:basis (template/basis)
                    :src-dirs src-dirs
                    :class-dir (template/class-dir)})))

(defn jar [{:keys [template artifact version]
            :as params}]
  (write-pom params)
  (copy-src params)
  (copy-resources params)
  (b/jar {:class-dir (template/class-dir)
          :jar-file  (template/jar-file template artifact version)}))

(defn jar* [{:keys [compile-java?
                    compile-clj?]
            :as params}]
  (clean)
  (when compile-java? (compile-java params))
  (when compile-clj?
    (compile-clj params))

  (jar params))

(defn install [{:keys [template artifact version]
                :as _params}]
  (b/install {:basis    (template/basis)
              :lib       (template/lib template artifact )
              :version   version
              :jar-file  (template/jar-file template artifact version)
              :class-dir (template/class-dir)}))

(defn install* [params]
  (jar* params)
  (install* params))

(defn uber [{:keys [template artifact version
                    main]
             :as params}]
  {:pre [(some? main)]}
  (write-pom params)
  (copy-src params)
  (copy-resources params)
  (b/uber {:class-dir (template/class-dir)
           :uber-file (template/uberjar-file template artifact version)
           :basis (template/basis)
           :main main}))

(defn uber* [{:keys [compile-java?
                     compile-clj?]
              ;; 默认 AOT
              :or {compile-clj? true}
             :as params}]
  (clean)
  (when compile-java? (compile-java params))
  (when compile-clj?
    (compile-clj params))
  (uber params))