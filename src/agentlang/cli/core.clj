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


(defn read-model [dirname]
  (let [model-filename (str dirname "/model.fractl")
        ^File model-file (io/file model-filename)]
    ;; Recognized model.fractl keys:
    ;; :name :version :fractl-version :components :dependencies
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


(defn rewrite-fractl-version [version]
  (if (contains? #{:current "current" nil} version)
    "0.5.4"
    version))


(defn find-dependencies [model-map]
  (let [fver (rewrite-fractl-version (:fractl-version model-map))
        deps (:dependencies model-map [])]
    (cond
      (nil? fver) (util/err-println "ERROR: Fractl version is unspecified in model.fractl")
      (not (string? fver)) (util/err-println "Fractl version is not a string in model.fractl")
      :else
      (->> deps
           (cons ['com.github.fractl-io/fractl fver])
           vec))))


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


(defn run-fractl [^String dirname classpath command args]
  (let [java-cmd (or (System/getenv "JAVA_CMD") "java")
        ^List
        pb-args (concat [java-cmd "-cp" classpath "fractl.core" command]
                        args)
        pb (-> (ProcessBuilder. pb-args)
               (.directory (File. dirname))
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
