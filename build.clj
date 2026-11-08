(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'top.kzre/clj-build-template)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def id-repo "huixiangchen0314/kzre-clj-build-template")
(defn clean [_]
      (b/delete {:path "target"}))

(defn- copy-src [_]
       (b/copy-dir {:src-dirs   ["src"]
                    :target-dir class-dir}))

(defn pom [_]
      (b/write-pom {:class-dir class-dir
                    :lib       lib
                    :version   version
                    :basis     @basis
                    :src-dirs  ["src"]
                    :scm       {:url                 (str "https://github.com/" id-repo)
                                :connection          (str "scm:git:git://github.com/" id-repo ".git")
                                :developerConnection (str "scm:git:git://github.com/" id-repo ".git")
                                :tag                 (str "v" version)}
                    :pom-data  [[:description "kzre-clj-build-template"]
                                [:url          (str "https://github.com/" id-repo)]
                                [:licenses
                                 [:license
                                  [:name "MIT License"]
                                  [:url  "https://opensource.org/license/mit/"]]]]}))

(defn jar [_]
      (clean nil)
      (copy-src nil)
      (pom nil)                            ; 必须先写 POM，再打包
      (b/jar {:class-dir class-dir
              :jar-file  jar-file}))

(defn install [_]
      (jar nil)                            ; 先构建 jar（含 POM）
      (b/install {:basis     @basis
                  :lib       lib
                  :version   version
                  :jar-file  jar-file
                  :class-dir class-dir}))