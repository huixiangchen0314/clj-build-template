(ns top.kzre.clj-build-template.template
  "构建模板配置"
  (:require
   [clojure.tools.build.api :as b]
   [top.kzre.clj-build-template.git :as git]
   [top.kzre.clj-build-template.github :as github]))

(defn create-template []
  {:java-version "8"
   :src-dirs ["src"]
   :resource-dirs ["resources"]
   :github-account (github/account)
   :github-url (str "https://github.com" (github/account))
   :group (git/group-id)})

(defn dist-dir [] "target")

(defn class-dir []  (str (dist-dir) "/classes"))

(defn lib [template artifact]
  (symbol (name (:group template))
          (name artifact)))

(defn jar-file [template artifact version]
  (format "target/%s-%s.jar" (name (lib template artifact)) version))
(defn uberjar-file [template artifact version]
  (format "target/%s-%s-standalone.jar" (name (lib template artifact)) version))

(defn github-repo-url [template artifact]
  (str (:github-url template) "/" (name artifact)))

(defn github-repo-id [template artifact]
 (str (:github-account template) "/" (name artifact)) )

(defn github-scm [tempalte artifact]
  (str "scm:git:git://github.com/" (github-repo-id tempalte artifact) ".git"))

(defn- basis* []
  (b/create-basis {:project "deps.edn"}))

(def basis (memoize basis*))

(defn pom-params [template artifact version]
  (let [id-repo (github-repo-url template artifact)]
    {:class-dir (class-dir)
     :lib       (lib template artifact)
     :version   version
     :basis     (basis)
     :src-dirs  ["src"]
     :scm       {:url                 (str "https://github.com/" id-repo)
                 :connection          (str "scm:git:git://github.com/" id-repo ".git")
                 :developerConnection (str "scm:git:git://github.com/" id-repo ".git")
                 :tag                 (str "v" version)}
     :pom-data  [[:description "kzre-clj-build-template"]
                 [:url          (str "https://github.com/" id-repo)]
                 ]} ))