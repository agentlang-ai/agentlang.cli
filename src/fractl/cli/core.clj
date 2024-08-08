(ns fractl.cli.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [cemerick.pomegranate.aether :as aether]
            [fractl.cli.util :as util])
  (:import (java.io File)
           (java.util List)))

(set! *warn-on-reflection* true)

(defn read-model []
  (let [model-filename "model.fractl"
        ^File model-file (io/file model-filename)]
    ;; Recognized model.fractl keys:
    ;; :name :version :fractl-version :components :dependencies
    ;;
    (when-not (.exists model-file) (ex-info "File does not exist" {:file model-filename}))
    (when-not (.isFile model-file) (ex-info "Not a file" {:file model-filename}))
    (when-not (.canRead model-file) (ex-info "File is unreadable" {:file model-filename}))
    (-> model-file
        io/reader
        slurp
        (string/replace "'[[" "[[")  ; unquote dependency list to make valid EDN
        edn/read-string)))

(defn find-dependencies [model-map]
  (let [fver (:fractl-version model-map)
        deps (:dependencies model-map [])]
    (->> deps
         (cons ['com.github.fractl-io/fractl fver])
         vec)))

(defn fetch-dependencies
  [deps]
  (reduce (fn [dep-filenames each-dep]
            (util/err-print "Fetching dependency: ")
            (util/err-prn each-dep)
            (->> (aether/resolve-dependencies :coordinates [each-dep])
                 aether/dependency-files
                 (map str)
                 (concat dep-filenames)))
          []
          deps))

(defn prepare-classpath
  [dep-filenames]
  (->> (distinct dep-filenames)
       (string/join File/pathSeparator)))

(defn run-app [classpath args]
  (let [java-cmd (or (System/getenv "JAVA_CMD") "java")
        ^List
        pb-args (concat [java-cmd "-cp" classpath "fractl.core" "run"]
                        args)
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

(defn command-deps []
  (-> (read-model)
      find-dependencies
      fetch-dependencies))

(defn command-depstree []
  (let [deps (->> (read-model)
                  find-dependencies)]
    (->> (aether/resolve-dependencies :coordinates deps)
         (aether/dependency-hierarchy deps)
         (walk/postwalk (fn [form] (if (map? form)
                                     (reduce-kv (fn [result k v]
                                                  (if (nil? v)
                                                    (conj result k)
                                                    (conj result (conj k v))))
                                                [] form)
                                     form)))
         pp/pprint)))

(defn command-classpath []
  (let [classpath (-> (read-model)
                      find-dependencies
                      fetch-dependencies
                      prepare-classpath)]
    (println classpath)))

(defn command-run [args]
  (let [classpath (-> (read-model)
                      find-dependencies
                      fetch-dependencies
                      prepare-classpath)]
    (util/err-println "Running app")
    (run-app classpath args)))

(defn command-help []
  (binding [*out* *err*]
    (util/err-println "Args: deps|classpath|run [command-args]")))

(defn process-args
  [& [command & args]]
  (case command
    "deps"      (command-deps)
    "depstree"  (command-depstree)
    "classpath" (command-classpath)
    "run"       (command-run args)
    (command-help)))
