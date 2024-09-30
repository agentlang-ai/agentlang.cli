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
(def ^:const git-deps-directory "deps/git")
(def ^:const baseline-version "0.6.0-alpha2")


(defn read-model [dirname]
  (let [model-filename (str dirname "/model.al")
        ^File model-file (io/file model-filename)]
    ;; Recognized model.al keys:
    ;; :name :version :agentlang-version :components :dependencies
    ;;
    (cond
      (not (.exists model-file)) (util/err-println (format "ERROR: File %s does not exist"
                                                           model-filename))
      (not (.isFile model-file)) (util/err-println (format "ERROR: %s is not a file"
                                                           model-filename))
      (not (.canRead model-file)) (util/err-println (format "ERROR: File %s is not readable"
                                                            model-filename))
      :else
      (-> model-file
          io/reader
          slurp
          (string/replace "'[[" "[[")  ; unquote dependency list to make valid EDN
          edn/read-string))))


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


(defn resolve-git-dependencies [repo-uri]
  (.mkdirs (io/file git-deps-directory))                    ; create Git deps base path if absent
  (let [repo-name (util/git-repo-uri->repo-name repo-uri)
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
  (let [analyze-dependency (fn [given-dependency] (prn "Analyzing:" given-dependency) (flush)
                             (let [[id target & more] given-dependency]
                               (cond
                                 (symbol? id) {:jar-deps [given-dependency]}
                                 (= :fs id)   (discover-dependencies target)
                                 (= :git id)  (-> target
                                                  resolve-git-dependencies
                                                  discover-dependencies)
                                 :otherwise   (util/throw-ex-info
                                                "Unsupported dependency type"
                                                {:dependency given-dependency}))))]
    (->> raw-deps
         (reduce (fn [{:keys [jar-deps
                              src-paths]} raw-dependency]
                   (let [{new-jar-deps :jar-deps
                          new-src-paths :src-paths} (analyze-dependency raw-dependency)]
                     {:jar-deps (concat jar-deps new-jar-deps)
                      :src-paths (concat src-paths new-src-paths)}))
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
