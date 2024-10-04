(ns agentlang.cli.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cemerick.pomegranate.aether :as aether]
            [agentlang.cli.util :as util])
  (:import (java.io File)
           (java.util List)))


(set! *warn-on-reflection* true)


(def ^:const current-directory ".")
(def ^:const model-filename "model.al")
(def ^:const git-deps-directory "deps/git")
(def ^:const baseline-version "0.6.0-alpha2")

(def ^:const env-var-github-username "GITHUB_USERNAME")
(def ^:const env-var-github-token "GITHUB_TOKEN")

(defmacro defenvar
  [varsym env-varname]
  (assert (symbol? varsym))
  (assert (string? env-varname))
  `(def ~varsym (let [ev# ~env-varname]
                  (first {ev# (System/getenv ev#)}))))

(defenvar envar-git-deps-inject-token "GIT_DEPS_INJECT_TOKEN")
(defenvar envar-github-username "GITHUB_USERNAME")
(defenvar envar-github-token    "GITHUB_TOKEN")


(defn model-dir-error [dirname]
  (let [model-filepath (str dirname "/" model-filename)
        ^File model-file (io/file model-filepath)]
    (cond
      (not (.exists model-file))  (format "File %s does not exist" model-filename)
      (not (.isFile model-file))  (format "%s is not a file" model-filename)
      (not (.canRead model-file)) (format "File %s is not readable" model-filename))))


(defn read-model [dirname]
  (let [model-filename (str dirname "/model.al")
        ^File model-file (io/file model-filename)
        unquote-deps (fn [model]
                       (if (contains? model :dependencies)
                         (let [deps (:dependencies model)]
                           (if (and (list? deps)
                                    (= 'quote (first deps))
                                    (= 2 (count deps)))
                             (update model :dependencies second)
                             model))
                         model))]
    ;; Recognized model.al keys:
    ;; :name :version :agentlang-version :components :dependencies
    ;;
    (if-let [error-message (model-dir-error dirname)]
      (util/throw-ex-info error-message {:error :model-dir-error})
      (-> model-file
          io/reader
          slurp
          (string/replace "'[[" "[[")  ; unquote dependency list to make valid EDN
          edn/read-string
          unquote-deps))))


(defn rewrite-agentlang-version [version]
  (if (contains? #{:current "current" nil} version)
    baseline-version
    version))


(defn find-dependencies [model-map]
  (let [fver (rewrite-agentlang-version (:agentlang-version model-map))
        deps (:dependencies model-map [])]
    (cond
      (nil? fver) (util/err-println "ERROR: AgentLang version is unspecified in model.al")
      (not (string? fver)) (util/err-println "AgentLang version is not a string in model.al")
      :else
      (->> deps
           (cons ['com.github.agentlang-ai/agentlang fver])
           vec))))


(declare discover-dependencies)
(declare clarify-dependencies)
(declare run-git-clone)


(defn expand-github-repo-uri [github-repo-uri]
  (let [github-repo-uri-regex #"https://github.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)(?:.git)?"
        [_ github-org-name
         github-repo-name] (re-matches github-repo-uri-regex github-repo-uri)
        github-username (val envar-github-username)
        github-pa-token (val envar-github-token)]
    (if (every? some? [github-org-name
                       github-repo-name
                       github-username
                       github-pa-token])
      (format "https://%s:%s@github.com/%s/%s.git"
              github-username
              github-pa-token
              github-org-name
              github-repo-name)
      (do
        (when-not github-org-name  (util/err-println "Cannot extract Org-name from Github repo:" github-repo-uri))
        (when-not github-repo-name (util/err-println "Cannot extract Repo-name from Github repo:" github-repo-uri))
        (when-not github-username  (util/err-println "Missing ENV Var" (key envar-github-username)
                                                     "for Github repo:" github-repo-uri))
        (when-not github-pa-token  (util/err-println "Missing ENV Var" (key envar-github-token)
                                                     "for Github repo:" github-repo-uri))
        github-repo-uri))))


(defn resolve-git-dependency [repo-uri]
  (.mkdirs (io/file git-deps-directory))                    ; create Git deps base path if absent
  (let [repo-uri  (let [git-deps-inject-token? (val envar-git-deps-inject-token)]
                    (if (and git-deps-inject-token?
                             (not= "false" git-deps-inject-token?))
                      (expand-github-repo-uri repo-uri)
                      (do
                        (util/err-println "To inject Github token to fetch dependency, define ENV var"
                                          (str "`" (key envar-git-deps-inject-token) "=true`"))
                        repo-uri)))
        repo-name (util/git-repo-uri->repo-name repo-uri)
        repo-path (str git-deps-directory "/" repo-name)]
    (when-not (.exists (io/file repo-path))
      (run-git-clone repo-uri repo-path))
    repo-path))


(defn discover-dependencies
  "Return {:jar-deps [] :src-paths []} for a given model (dir)."
  [model-dir]
  (let [app-model (read-model model-dir)
        raw-deps (find-dependencies app-model)
        {:keys [jar-deps
                src-paths]} (clarify-dependencies raw-deps)]
    {:app-model app-model
     :jar-deps jar-deps
     :src-paths (-> model-dir
                    (cons src-paths))}))


(defn clarify-dependencies
  "Return {:jar-deps [] :src-paths []} for a given set of raw dependencies."
  [raw-deps]
  (let [analyze-dependency (fn [given-dependency]
                             (util/err-println "Analyzing dependency:" (pr-str given-dependency))
                             (let [[id target & more] given-dependency]
                               (cond
                                 (symbol? id) {:jar-deps [given-dependency]}
                                 (= :fs id)   (discover-dependencies target)
                                 (= :git id)  (-> target
                                                  resolve-git-dependency
                                                  discover-dependencies)
                                 :otherwise   (util/throw-ex-info
                                                "Unsupported dependency type"
                                                {:dependency given-dependency}))))]
    (->> raw-deps
         (reduce (fn [{:keys [jar-deps
                              src-paths]} raw-dependency]
                   (let [{new-jar-deps :jar-deps
                          new-src-paths :src-paths} (analyze-dependency raw-dependency)]
                     {:jar-deps (distinct (concat jar-deps new-jar-deps))
                      :src-paths (distinct (concat src-paths new-src-paths))}))
                 {:jar-deps []
                  :src-paths []}))))


(defn resolve-dependencies [deps]
  (aether/resolve-dependencies
    :coordinates deps
    :repositories (merge aether/maven-central
                         {"clojars" "https://clojars.org/repo"})))


(defn fetch-dependencies
  [deps]
  (reduce (fn [dep-filenames each-dep]
            (util/err-print "Resolving dependency: ")
            (util/err-prn each-dep)
            (->> (resolve-dependencies [each-dep])
                 aether/dependency-files
                 (map str)
                 (concat dep-filenames)))
          []
          deps))


(defn prepare-classpath
  [dep-filenames]
  (->> (distinct dep-filenames)
       (string/join File/pathSeparator)))


(defn run-git-clone [git-repo-uri local-repo-name]
  (util/err-println "Cloning Git repo" git-repo-uri "into" local-repo-name)
  (let [^List
        pb-args ["git" "clone" git-repo-uri local-repo-name]
        pb (-> (ProcessBuilder. pb-args)
               (.inheritIO))
        p (.start pb)
        err (.errorReader p)
        out (.inputReader p)
        exit-value (promise)]
    (future
      (try
        (util/retry-if-interrupted
          (.waitFor p))
        (catch Throwable e
          (.printStackTrace e))
        (finally
          (deliver exit-value (.exitValue p)))))
    (while (not (realized? exit-value))
      (when-not (or (util/apply-err-out util/print-buffer err)
                    (util/print-buffer out))
        (util/sleep-millis 100)))
    @exit-value))


(defn run-agentlang [^String dirname sourcepath classpath command args]
  (let [java-cmd (or (System/getenv "JAVA_CMD") "java")
        ^List
        pb-args (concat [java-cmd "-cp" classpath "agentlang.core" command]
                        args)
        pb (-> (ProcessBuilder. pb-args)
               (.directory (File. dirname))
               (.inheritIO))
        _ (doto (.environment pb)
            (.put "AGENTLANG_MODEL_PATHS" sourcepath))
        p (.start pb)
        err (.errorReader p)
        out (.inputReader p)
        exit-value (promise)]
    (future
      (try
        (util/retry-if-interrupted
          (.waitFor p))
        (catch Throwable e
          (.printStackTrace e))
        (finally
          (deliver exit-value (.exitValue p)))))
    (while (not (realized? exit-value))
      (when-not (or (util/apply-err-out util/print-buffer err)
                    (util/print-buffer out))
        (util/sleep-millis 100)))
    @exit-value))
