(ns agentlang.cli.command
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [agentlang.cli.core :as core]
            [agentlang.cli.newproj :as newproj]
            [agentlang.cli.util :as util])
  (:import (java.io StringWriter)))


(set! *warn-on-reflection* true)


(defmacro when-model-dir [dirname & body]
  `(if-let [error-message# (core/model-dir-error ~dirname)]
     (do
       (util/err-println "ERROR:" error-message#)
       1)
     (do
       ~@body)))


(defn command-deps [dirname]
  (when-model-dir
    dirname
    (let [{:keys [app-model
                  jar-deps
                  src-paths]} (core/discover-dependencies dirname)]
      (core/fetch-dependencies jar-deps))))


(defn command-depstree [dirname]
  (when-model-dir
    dirname
    (let [{:keys [app-model
                  jar-deps
                  src-paths]} (core/discover-dependencies dirname)]
      (->> (core/resolve-dependencies jar-deps)
           (aether/dependency-hierarchy jar-deps)
           (walk/postwalk (fn [form] (if (map? form)
                                       (reduce-kv (fn [result k v]
                                                    (if (nil? v)
                                                      (conj result k)
                                                      (conj result (conj k v))))
                                                  [] form)
                                       form)))
           pp/pprint))))


(defn command-classpath [dirname]
  (when-model-dir
    dirname
    (let [classpath (-> (core/read-model dirname)
                        core/find-dependencies
                        core/fetch-dependencies
                        core/prepare-classpath)]
      (println classpath))))


(defn command-new [[new-type new-name]]
  (when-not (#{"app" "resolver"} new-type)
    (util/err-exit "You must supply new-type, e.g. 'app', 'resolver' etc."))
  (when-not new-name
    (util/err-exit "You must supply project-name"))
  (case new-type
    "app"      (if-let [error (util/invalid-project-name? new-name)]
                 (util/err-exit "Invalid app name:" error)
                 (do
                   (util/err-println "Creating new AgentLang app")
                   (newproj/create-new-app new-name)))
    "resolver" (if-let [error (util/invalid-project-name? new-name)]
                 (util/err-exit "Invalid resolver name:" error)
                 (do
                   (util/err-println "Creating new Agentlang resolver")
                   (util/err-exit "Not yet implemented")))))


(defn command-agentlang [dirname msg-prefix agentlang-command args]
  (let [{:keys [app-model
                jar-deps
                src-paths]} (core/discover-dependencies dirname)
        app-version    (:version app-model "(unknown app version)")
        agentlang-version (core/rewrite-agentlang-version (:agentlang-version app-model))
        sourcepath (string/join util/path-separator src-paths)
        classpath (-> jar-deps
                      core/fetch-dependencies
                      core/prepare-classpath)]
    (util/err-println (format "%s %s with AgentLang %s"
                              msg-prefix
                              app-version agentlang-version))
    (core/run-agentlang dirname sourcepath classpath agentlang-command args)))


(defn command-clone [[command repo-uri & args]]
  ;; [ Github ]
  ;; git clone https://oauth2:oauth-key-goes-here@github.com/username/repo.git
  ;; git clone https://username:token@github.com/username/repo.git
  ;; [ GitLab ]
  ;; git clone https://gitlab-ci-token:${Personal Access Tokens}@gitlab.com/username/myrepo.git
  ;; git clone https://oauth2:${Personal Access Tokens}@gitlab.com/username/myrepo.git
  (let [repo-name (util/git-repo-uri->repo-name repo-uri)
        git-result (core/run-git-clone repo-uri repo-name)]
    (if (zero? git-result)
      (let [{:keys [app-model
                    jar-deps
                    src-paths]} (core/discover-dependencies repo-name)
            sourcepath (string/join util/path-separator src-paths)
            classpath (-> jar-deps
                          core/fetch-dependencies
                          core/prepare-classpath)]
        (core/run-agentlang repo-name sourcepath classpath command args))
      git-result)))


(defn command-version [[version-format]]
  (let [cliapp-version (-> (io/resource "project.edn")
                        slurp
                        (edn/read-string)
                        :version)
        agentlang-version (when-not (core/model-dir-error core/current-directory)
                            (when-let [model (binding [*err* (StringWriter.)]
                                               (core/read-model core/current-directory))]
                              (:agentlang-version model)))
        clijvm-version (System/getProperty "java.version")
        version {:cli-version cliapp-version
                 :agentlang-version agentlang-version
                 :jvm-version clijvm-version}]
    (case (-> version-format
              str
              string/lower-case
              string/trim)
      "edn" (pp/pprint version)
      "json" (printf "{\"cli-version\": \"%s\",
 \"agentlang-version\": %s,
 \"jvm-version\": \"%s\"}\n"
                     cliapp-version
                     (and agentlang-version (format "\"%s\"" agentlang-version))
                     clijvm-version)
      (do
        (println "CLI version:" cliapp-version)
        (println "AgentLang version:" (or agentlang-version "Unavailable"))
        (println "JVM version:" clijvm-version)))
    (flush)))


(defn command-help []
  (binding [*out* *err*]
    (util/err-println "Syntax: agent <command> [command-args]

agent deps               Fetch dependencies for an AgentLang app
agent depstree           Print dependency-tree for an AgentLang app
agent classpath          Print classpath for an AgentLang app
agent clonenrepl         Clone a (Git) repo and start nREPL server in the app
agent clonerepl          Clone a (Git) repo and start REPL in the app
agent clonerun           Clone a (Git) repo and run the app
agent new app <ap-name>  Create a new AgentLang app
agent nrepl              Start an nREPL server
agent repl               Start a local REPL
agent run [run-args]     Run an AgentLang app
agent version [format]   Print agentlang.cli version (format: edn/json)")))


(defn process-command
  [& [command & args]]
  (let [result (case command
                 "deps" (command-deps core/current-directory)
                 "depstree" (command-depstree core/current-directory)
                 "classpath" (command-classpath core/current-directory)
                 "clonenrepl" (command-clone (cons "nrepl" args))
                 "clonerepl" (command-clone (cons "repl" args))
                 "clonerun" (command-clone (cons "run" args))
                 "help" (command-help)
                 "new" (command-new args)
                 "nrepl" (command-agentlang core/current-directory
                                            "Starting nREPL server for app"
                                            "nrepl" args)
                 "repl" (command-agentlang core/current-directory
                                           "Starting REPL for app"
                                           "repl" args)
                 "run" (command-agentlang core/current-directory
                                          "Starting app"
                                          "run" args)
                 "version" (command-version args)
                 (do
                   (if (nil? command)
                     (util/err-println "ERROR: No command passed")
                     (util/err-println "ERROR: Unrecognized command" command))
                   (command-help)))]
    (when (and (number? result)
               (integer? result))
      (System/exit result))))
