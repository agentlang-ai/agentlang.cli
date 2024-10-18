# agentlang.cli

CLI tool for AgentLang applications.

## Installation

You may download a binary distribution and decompress as follows:

```shell
$ tar xvf agentlang.cli-<version>-bin.tar.gz
```

Now, you may set PATH environment variable to the created directory and use the `agent` command.

You would need Java 21 and Git installed to use this CLI tool.


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


## Build from sources

### Install locally

You may build the sources and install the JAR locally as follows:

```shell
$ lein do clean, uberjar, prep, local
```

Include the `bin/agent` script in your PATH environment variable.

### Build distribution

To create a tarball distribution (under `target/dist` directory), run the following:

```shell
$ ./dist-build.sh
```

### Docker image

To create a Docker image, run the following:

```shell
$ ./docker-build.sh help  # commands: clean, build, push-version, push-latest
```


## License

Copyright © 2024 Fractl, Inc

This program and the accompanying materials are made available under the
terms of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.html.
