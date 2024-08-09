(ns fractl.cli.main
  (:require [fractl.cli.command :as command])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main
  "Main entrypoint for this app"
  [& args]
  (apply command/process-command args))
