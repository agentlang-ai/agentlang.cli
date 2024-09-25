# agentlang.cli

CLI tool for AgentLang applications.

## Installation

Install the JAR locally:

```shell
$ lein do clean, uberjar, prep, local
```

Include the `bin/agent` script in your PATH environment variable.

## Usage

Change into the AgentLang app directory and run the `agent` command:

```shell
$ cd <agentlang-app-dir>
$ agent help
$ agent classpath
$ agent run -c config.edn
```

## Docker usage

```shell
$ docker pull agentlang/agentlang.cli:latest
$ docker run --rm -it agentlang/agentlang.cli:latest agent version
```

### Create a new app

```shell
$ docker run --rm -v .:/agentlang -it agentlang/agentlang.cli:latest agent new app hello
$ cd hello
$ docker run --rm -v .:/agentlang -p 8080:8080 -it agentlang/agentlang.cli:latest agent run
```

Follow the generated `hello/README.md` file for instructions.

## License

Copyright © 2024 Fractl, Inc

This program and the accompanying materials are made available under the
terms of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.html.
