(ns agentlang.cli.newproj.emit
  (:require [clojure.java.io :as io]))


(defn emit-app-readme.md [project-name component-keyword]
  (format "# %s

FIXME: Description

## Usage

Start the app:

```shell
$ ftl run
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


(defn emit-app-model.al [component-keyword]
  (format "{:name %s
 :version \"0.0.1\"
 :agentlang-version \"0.6.0-alpha2\"
 :components [%s.Core]
 :dependencies []}"
          component-keyword
          component-keyword))


(defn emit-app-core.al [component-keyword]
  (let []
    (format "(component
  %s.Core)

(record :Response {:Message :String})

(dataflow :Greet
 {:Response {:Message '(str \"hello \" :Greet.Name)}})"
            component-keyword)))


(defn make-file [filepath content]
  (io/make-parents filepath)
  (spit filepath content))
