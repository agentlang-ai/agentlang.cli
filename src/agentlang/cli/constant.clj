(ns agentlang.cli.constant)


(set! *warn-on-reflection* true)


(def ^:const current-directory ".")
(def ^:const model-filename "model.al")
(def ^:const al-file-extension ".al")
(def ^:const git-deps-directory "deps/git")
(def ^:const baseline-version "0.6.1")

(def ^:const env-var-github-username "GITHUB_USERNAME")
(def ^:const env-var-github-token "GITHUB_TOKEN")

(def ^:const envvar-agentlang-model-paths "AGENTLANG_MODEL_PATHS")

(defmacro defenvar
  [varsym env-varname]
  (assert (symbol? varsym))
  (assert (string? env-varname))
  `(def ~varsym (let [ev# ~env-varname]
                  (first {ev# (System/getenv ev#)}))))

(defenvar envar-git-deps-inject-token "GIT_DEPS_INJECT_TOKEN")
(defenvar envar-github-username "GITHUB_USERNAME")
(defenvar envar-github-token    "GITHUB_TOKEN")
