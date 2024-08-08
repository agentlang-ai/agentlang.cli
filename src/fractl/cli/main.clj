(ns fractl.cli.main
  (:require [fractl.cli.core :as core])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main
  "Main entrypoint for this app"
  [& args]
  (apply core/process-args args))
