(ns fractl.cli.command
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [fractl.cli.core :as core]
            [fractl.cli.util :as util]))


(set! *warn-on-reflection* true)


(defn command-deps []
  (-> (core/read-model)
      core/find-dependencies
      core/fetch-dependencies))


(defn command-depstree []
  (let [deps (->> (core/read-model)
                  core/find-dependencies)]
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
  (let [classpath (-> (core/read-model)
                      core/find-dependencies
                      core/fetch-dependencies
                      core/prepare-classpath)]
    (println classpath)))


(defn command-run [args]
  (let [classpath (-> (core/read-model)
                      core/find-dependencies
                      core/fetch-dependencies
                      core/prepare-classpath)]
    (util/err-println "Running app")
    (core/run-app classpath args)))


(defn command-help []
  (binding [*out* *err*]
    (util/err-println "Args: deps|depstree|classpath|run [command-args]")))


(defn process-command
  [& [command & args]]
  (case command
    "deps"      (command-deps)
    "depstree"  (command-depstree)
    "classpath" (command-classpath)
    "run"       (command-run args)
    (command-help)))
