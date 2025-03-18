(ns agentlang.cli.command
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [agentlang.cli.constant :as const]
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
    (let [{:keys [app-model
                  jar-deps
                  src-paths]} (core/discover-dependencies dirname)
          classpath (-> jar-deps
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
                   (newproj/create-new-resolver new-name)))))


(defn command-agentlang [dirname msg-prefix agentlang-command args]
  (let [{:keys [app-model
                jar-deps
                src-paths]} (core/discover-dependencies dirname)
        app-version    (:version app-model "(unknown app version)")
        agentlang-version (core/rewrite-agentlang-version (:agentlang-version app-model))
        sourcepath (->> src-paths
                        (mapv util/make-parent-path)
                        (filter some?)
                        distinct
                        (mapv util/make-absolute-file-path)
                        (string/join util/path-separator))
        classpath (-> jar-deps
                      core/fetch-dependencies
                      core/prepare-classpath)]
    (util/err-println (format "%s %s with AgentLang %s"
                              msg-prefix
                              app-version agentlang-version))
    (core/run-agentlang dirname sourcepath classpath agentlang-command args)))


(defn script-execution? [args]
  (let [script-name (last args)]
    (and script-name
         (string/ends-with? script-name const/al-file-extension)
         args)))


(defn execute-script [dirname args]
  (let [jar-deps (core/find-dependencies {:agentlang-version const/baseline-version})
        classpath (-> jar-deps
                      core/fetch-dependencies
                      core/prepare-classpath)]
    (core/run-agentlang dirname nil classpath nil args)))


(defn command-run [dirname msg-prefix agentlang-command args]
  (if (script-execution? args)
    (execute-script dirname args)
    (command-agentlang dirname msg-prefix agentlang-command args)))


(defn command-clone [[command repo-uri & args]]
  ;; [ Github ]
  ;; git clone https://oauth2:oauth-key-goes-here@github.com/username/repo.git
  ;; git clone https://username:token@github.com/username/repo.git
  ;; [ GitLab ]
  ;; git clone https://gitlab-ci-token:${Personal Access Tokens}@gitlab.com/username/myrepo.git
  ;; git clone https://oauth2:${Personal Access Tokens}@gitlab.com/username/myrepo.git
  (let [repo-name (util/git-repo-uri->repo-name repo-uri)
        git-result (core/run-git-clone (util/parse-repo-uri repo-uri) repo-name)]
    (if (zero? git-result)
      (let [{:keys [app-model
                    jar-deps
                    src-paths]} (core/discover-dependencies repo-name)
            sourcepath (->> src-paths
                            (mapv util/make-parent-path)
                            (filter some?)
                            distinct
                            (mapv util/make-absolute-file-path)
                            (string/join util/path-separator))
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
        agentlang-version (when-not (core/model-dir-error const/current-directory)
                            (when-let [model (binding [*err* (StringWriter.)]
                                               (core/read-model const/current-directory))]
                              (:agentlang-version model)))
        effective-version (or agentlang-version const/baseline-version)
        clijvm-version (System/getProperty "java.version")
        version {:cli-version cliapp-version
                 :agentlang-version effective-version
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
                     (and effective-version (format "\"%s\"" effective-version))
                     clijvm-version)
      (do
        (println "CLI version:" cliapp-version)
        (println "AgentLang version:" effective-version)
        (println "JVM version:" clijvm-version)))
    (flush)))


(defn command-help []
  (binding [*out* *err*]
    (util/err-println "Syntax: agent <command> [command-args]

agent deps                          Fetch dependencies for an AgentLang app
agent depstree                      Print dependency-tree for an AgentLang app
agent classpath                     Print classpath for an AgentLang app
agent clonenrepl <git-url>          Clone a (Git) repo and start nREPL server in the app
agent clonerepl <git-url>           Clone a (Git) repo and start REPL in the app
agent clonerun <git-url> [args]     Clone a (Git) repo and run the app
agent new <project-type> <name>     Create a new AgentLang app/resolver (type: app/resolver)
agent nrepl                         Start an nREPL server
agent repl                          Start a local REPL
agent run [run-args]                Run an AgentLang app or script
agent doc                           Generate OpenAPI and Swagger docs for the app
agent migrate MODEL-NAME [git/local] [branch/path]         Migrate database given previous version of the app
agent version [format]              Print agentlang.cli version (format: edn/json)
agent [options] <path/to/script.al> Run an AgentLang script")))


(defn process-command
  [& [command & args]]
  (let [result (case command
                 "deps" (command-deps const/current-directory)
                 "depstree" (command-depstree const/current-directory)
                 "classpath" (command-classpath const/current-directory)
                 "clonenrepl" (command-clone (cons "nrepl" args))
                 "clonerepl" (command-clone (cons "repl" args))
                 "clonerun" (command-clone (cons "run" args))
                 "help" (command-help)
                 "new" (command-new args)
                 "nrepl" (command-agentlang const/current-directory
                                            "Starting nREPL server for app"
                                            "nrepl" args)
                 "repl" (command-agentlang const/current-directory
                                           "Starting REPL for app"
                                           "repl" args)
                 "run" (command-run const/current-directory
                                    "Starting app"
                                    "run" args)
                 "doc" (command-run const/current-directory
                                    "Generating documentation"
                                    "doc" args)
                 "migrate" (command-run const/current-directory
                                        "Migrating database"
                                        "migrate" args)
                 "version" (command-version args)
                 nil (do
                       (util/err-println "ERROR: No command passed")
                       (command-help))
                 ;; command is non-nil now
                 (if-let [script-args (script-execution? (cons command args))]
                   (execute-script const/current-directory script-args)
                   (do
                     (util/err-println "ERROR: Unrecognized command" command)
                     (command-help))))]
    (when (and (number? result)
               (integer? result))
      (System/exit result))))
