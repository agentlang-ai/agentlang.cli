#!/usr/bin/env bash

###
## Script to build/push Docker image for agentlang.cli
##
## ./docker-build.sh help

set -e

CTR_NAME="agentlang.cli"
IMG_NAME="agentlang.cli"
IMG_REPO="agentlang/agentlang.cli"

# Print to STDERR
function err {
  printf >&2 "$1 $2 $3 $4 $5 $6 $7 $8\n"
}

function getContainerId() {
  docker ps -a | grep $CTR_NAME | tr -s ' ' | cut -d ' ' -f 1
}

function getImageId() {
  docker images | grep $IMG_NAME | tr -s ' ' | cut -d ' ' -f 3
}

function clean() {
  containerId=$(getContainerId)
  if [ ! -z "$containerId" ]; then
    docker kill $containerId || true
    docker rm -f $containerId
  fi

  imageId=$(getImageId)
  if [ ! -z "$imageId" ]; then
    docker rmi -f $imageId
  fi
}

function yesOrNo {
    while true; do
        read -p "$* [y/n]: " yn
        case $yn in
            [Yy]*) return 0  ;;
            [Nn]*) err "Aborted" ; return  1 ;;
        esac
    done
}

function findVersion() {
    cat project.clj | head --lines=1 | tr -s ' ' | cut -d ' ' -f 3 | tr -d '"'
}

function buildImage() {
    lein do clean, test, uberjar
    yesOrNo "Build local docker image $IMG_NAME" && \
      docker build -f Dockerfile --build-arg CACHE_BUST=$(date +%s) -t $IMG_NAME .
}

function pushToDockerHub() {
    TAG_NAME="$1"
    yesOrNo "Tag image as ${IMG_REPO}:${TAG_NAME}" && \
      docker tag $IMG_NAME:latest $IMG_REPO:$TAG_NAME && \
      yesOrNo "Push image ${IMG_REPO}:${TAG_NAME}" && \
      docker login && \
      docker push $IMG_REPO:$TAG_NAME
}

function pushVersion() {
    pushToDockerHub "$(findVersion)"
}

function pushLatest() {
    pushToDockerHub "latest"
}

case "$1" in
build) buildImage;;
clean) clean;;
push-version) pushVersion;;
push-latest) pushLatest;;
*) err "Syntax: $0 build | clean | push-version | push-latest"
esac
