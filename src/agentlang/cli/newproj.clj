(ns agentlang.cli.newproj
  (:require [clojure.string :as string]
            [agentlang.cli.newproj.emit :as emit]
            [agentlang.cli.util :as util]))


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
        component-keyword (util/project-name->component-keyword app-name)]
    (emit/make-file (format "%s/README.md"
                            project-name) (emit/emit-app-readme.md project-name
                                                                   component-keyword))
    (emit/make-file (format "%s/model.al"
                            project-name) (emit/emit-app-model.al component-keyword))
    (emit/make-file (format "%s/%s/core.al"
                            project-name
                            (util/project-name->component-dirname
                              app-name)) (emit/emit-app-core.al component-keyword))))


(defn create-new-resolver [resolver-name]
  (abort-if-file-exists! resolver-name)
  (let [project-name (string/lower-case resolver-name)
        component-keyword (util/project-name->component-keyword resolver-name)]
    (emit/make-file (format "%s/README.md"
                            project-name) (emit/emit-resolver-readme.md project-name
                                                                        component-keyword))
    (emit/make-file (format "%s/model.al"
                            project-name) (emit/emit-app-model.al component-keyword))
    (emit/make-file (format "%s/%s/core.al"
                            project-name
                            (util/project-name->component-dirname
                              resolver-name)) (emit/emit-resolver-core.al component-keyword))))
