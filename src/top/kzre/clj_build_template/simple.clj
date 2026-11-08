(ns top.kzre.clj-build-template.simple
  "纯clojure项目"
  (:require
    [clojure.string :as str]
    [clojure.tools.build.api :as b]
    [top.kzre.clj-build-template.template :as template]))

(defn create-template []
  ((requiring-resolve 'top.kzre.clj-build-template.template/create-template)))

(defn clean []
  (b/delete {:path (template/dist-dir)}))

(defn- copy-src
  [{:keys [template src-dirs src-exts]
    :or   {src-exts [".clj" ".cljs" ".cljc" ".cljh"]}
    :as   _params}]
  (let [exts (mapv #(str/replace % #"^\." "") src-exts)  ; 去掉前导点
        glob (if (= 1 (count exts))
               (str "**/*." (first exts))                 ; 单后缀不加大括号
               (str "**/*.{" (str/join "," exts) "}"))]
    (b/copy-dir {:src-dirs   (or src-dirs (:src-dirs template))
                 :target-dir (template/class-dir)
                 :include    glob})))

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