# fractl.cli

CLI tool for Fractl applications.

## Installation

Install the JAR locally:

```shell
$ lein do clean, uberjar, local
```

Include the `bin/ftl` script in your PATH environment variable.

## Usage

Change into the Fractl app directory and run the `ftl` command:

```shell
$ cd <fractl-app-dir>
$ ftl help
$ ftl classpath
$ ftl run -c config.edn
```

## Docker usage

```shell
$ docker pull fractlio/fractl.cli:latest
$ docker run --rm -it fractlio/fractl.cli:latest ftl version
```

### Create a new app

```shell
$ docker run --rm -v .:/fractl -it fractlio/fractl.cli:latest ftl new app hello
$ cd hello
$ docker run --rm -v .:/fractl -p 8080:8080 -it fractlio/fractl.cli:latest ftl run
```

Follow the generated `hello/README.md` file for instructions.

## License

Copyright © 2024 Fractl, Inc

This program and the accompanying materials are made available under the
terms of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.html.
