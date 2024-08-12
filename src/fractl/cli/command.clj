(ns fractl.cli.command
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [fractl.cli.core :as core]
            [fractl.cli.newproj :as newproj]
            [fractl.cli.util :as util]))


(set! *warn-on-reflection* true)


(defn command-deps []
  (or (some-> (core/read-model)
              core/find-dependencies
              core/fetch-dependencies)
      1))


(defn command-depstree []
  (if-let [model (core/read-model)]
    (let [deps (core/find-dependencies model)]
      (->> (core/resolve-dependencies deps)
           (aether/dependency-hierarchy deps)
           (walk/postwalk (fn [form] (if (map? form)
                                       (reduce-kv (fn [result k v]
                                                    (if (nil? v)
                                                      (conj result k)
                                                      (conj result (conj k v))))
                                                  [] form)
                                       form)))
           pp/pprint))
    1))


(defn command-classpath []
  (if-let [model (core/read-model)]
    (let [classpath (-> model
                        core/find-dependencies
                        core/fetch-dependencies
                        core/prepare-classpath)]
      (println classpath))
    1))


(defn command-new [[new-type new-name]]
  (when-not (#{"app" "resolver"} new-type)
    (util/err-exit "You must supply new-type, e.g. 'app', 'resolver' etc."))
  (when-not new-name
    (util/err-exit "You must supply project-name"))
  (case new-type
    "app"      (if-let [error (util/invalid-project-name? new-name)]
                 (util/err-exit "Invalid app name:" error)
                 (do
                   (util/err-println "Creating new Fractl app")
                   (newproj/create-new-app new-name)))
    "resolver" (if-let [error (util/invalid-project-name? new-name)]
                 (util/err-exit "Invalid resolver name:" error)
                 (do
                   (util/err-println "Creating new Fractl resolver")
                   (util/err-exit "Not yet implemented")))))


(defn command-fractl [msg-prefix fractl-command args]
  (let [app-model      (core/read-model)
        app-version    (:version app-model "(unknown app version)")
        fractl-version (:fractl-version app-model "(unknown Fractl version)")
        classpath (-> app-model
                      core/find-dependencies
                      core/fetch-dependencies
                      core/prepare-classpath)]
    (util/err-println (format "%s %s with Fractl %s"
                              msg-prefix
                              app-version fractl-version))
    (core/run-fractl classpath fractl-command args)))


(defn command-version []
  (let [version (-> (io/resource "project.edn")
                    slurp
                    (edn/read-string)
                    :version)]
    (println version)))


(defn command-help []
  (binding [*out* *err*]
    (util/err-println "Syntax: ftl <command> [command-args]

ftl deps               Fetch dependencies for a Fractl app
ftl depstree           Print dependency-tree for a Fractl app
ftl classpath          Print classpath for a Fractl app
ftl new app <ap-name>  Create a new Fractl app
ftl nrepl              Start an nREPL server
ftl repl               Start a local REPL
ftl run [run-args]     Run a Fractl app
ftl version            Print ftl version")))


(defn process-command
  [& [command & args]]
  (let [result (case command
                 "deps" (command-deps)
                 "depstree" (command-depstree)
                 "classpath" (command-classpath)
                 "new" (command-new args)
                 "nrepl" (command-fractl "Starting nREPL server for app"
                                         "nrepl" args)
                 "repl" (command-fractl "Starting REPL for app"
                                        "repl" args)
                 "run" (command-fractl "Starting app"
                                       "run" args)
                 "version" (command-version)
                 (command-help))]
    (when (and (number? result)
               (integer? result))
      (System/exit result))))
