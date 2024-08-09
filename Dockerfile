FROM clojure:temurin-21-lein-jammy

ENV DOCKER_CONTAINER=Yes

COPY bin/ftl /usr/local/bin/
RUN mkdir -p /root/.fractl/self-installs
COPY target/uberjar/*-standalone.jar /root/.fractl/self-installs

RUN apt update && apt install -y git wget iproute2
RUN /usr/local/bin/ftl

WORKDIR /tmp
RUN ftl new app hello && cd hello && ftl classpath

WORKDIR /fractl
