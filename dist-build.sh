#!/usr/bin/env bash

set -e

CLI_VERSION=0.6.0

DIST_HOME=target/dist
UNIX_DIST_BASE=$DIST_HOME/unix
UNIX_DIST_NAME=agentlang.cli-${CLI_VERSION}-bin
UNIX_DIST_HOME=$UNIX_DIST_BASE/${UNIX_DIST_NAME}
UNIX_TARBALL_NAME=$UNIX_DIST_BASE/${UNIX_DIST_NAME}.tar.gz
UBERJAR_PATH=target/uberjar/agentlang.cli-${CLI_VERSION}-standalone.jar

function createUnix() {
  lein do clean, test, uberjar
  mkdir -p $UNIX_DIST_HOME
  cp bin/agent $UNIX_DIST_HOME
  cp $UBERJAR_PATH $UNIX_DIST_HOME
  cp README.md $UNIX_DIST_HOME
  cp LICENSE $UNIX_DIST_HOME
  tar czvf $UNIX_TARBALL_NAME -C $UNIX_DIST_BASE ${UNIX_DIST_NAME}
  echo "Created Unix distribution: $UNIX_TARBALL_NAME"
}

createUnix
