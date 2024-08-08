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

## License

Copyright © 2024 Fractl, Inc

This program and the accompanying materials are made available under the
terms of the Apache License 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.html.
