(ns agentlang.cli.newproj.emit
  (:require [clojure.java.io :as io]
            [agentlang.cli.constant :as const]))


(defn emit-app-readme.md [project-name component-keyword]
  (format "# %s

FIXME: Description

## Usage

Start the app:

```shell
$ agent run
```

In another terminal:

```shell
$ curl -X POST \\
    -H 'Content-Type: application/json' \\
    -d '{\"%s.Core/Greet\": {\"Name\": \"Fred\"}}' \\
    http://localhost:8080/api/%s.Core/Greet
```

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.html.
"
          project-name
          (name component-keyword)
          (name component-keyword)))


(defn emit-resolver-readme.md [project-name component-keyword]
  (format "# %s

FIXME: Description

## Usage

Include as dependency in your app `model.al`:

```clojure
:dependencies [; other dependencies
               [:fs \"%s\"]  ; local filesystem path
               ]
```

Refer from application code:

```clojure
(component
  :AppName.Core
  {:refer [%s.Core]})
```

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.html.
"
          project-name
          project-name
          (name component-keyword)))


(defn emit-app-model.al [component-keyword]
  (format "{:name %s
 :version \"0.0.1\"
 :agentlang-version \"%s\"
 :components [%s.Core]
 :dependencies []}"
          component-keyword
          const/baseline-version
          component-keyword))


(defn emit-app-core.al [component-keyword]
  (let []
    (format "(component
  %s.Core)

(record :Response {:Message :String})

(dataflow :Greet
 {:Response {:Message (quote (str \"hello \" :Greet.Name))}})"
            component-keyword)))


(defn emit-resolver-core.al [component-keyword]
  (let []
    (format "(component
  %s.Core
  {:clj-import (quote [(:require [agentlang.component :as cn])])})

(entity :Message
  {:Greeting :String})

(defn create-entity [instance]
  ;; put your \"create\" logic here
  instance)

(defn get-entity [[[_ entity-name] {where :where}]]
  (when (= :Message entity-name)
    ;; put your \"get\" logic here
    [(cn/make-instance %s.Core/Message {:Greeting \"Hello\"})]))

(resolver
  %s.Core/Resolver
  {:with-methods {:create create-entity
                  :query get-entity}
   :paths [%s.Core/Message]})"
            component-keyword
            component-keyword
            component-keyword
            component-keyword)))


(defn make-file [filepath content]
  (io/make-parents filepath)
  (spit filepath content))
