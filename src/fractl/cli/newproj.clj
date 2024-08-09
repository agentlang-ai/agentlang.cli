(ns fractl.cli.newproj
  (:require [clojure.string :as string]
            [fractl.cli.newproj.emit :as emit]
            [fractl.cli.util :as util]))


(defn abort-if-file-exists! [project-name]
  (when (util/file-exists? project-name)
    (-> (util/file-type project-name)
        string/capitalize
        (format "%s %s already exists. Cannot create project"
                project-name)
        util/err-exit)))


(defn create-new-app [app-name]
  (abort-if-file-exists! app-name)
  (let [project-name (string/lower-case app-name)
        component-name (keyword (string/capitalize app-name))]
    (emit/make-file (format "%s/README.md"
                            project-name) (emit/emit-app-readme.md project-name
                                                                   component-name))
    (emit/make-file (format "%s/model.fractl"
                            project-name) (emit/emit-app-model.fractl component-name))
    (emit/make-file (format "%s/%s/core.fractl"
                            project-name
                            project-name) (emit/emit-app-core.fractl component-name))))
