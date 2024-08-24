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

ENV CLONE_CMD=FIXME
ENV CLONE_URI=FIXME

## See: https://stackoverflow.com/a/58837546
CMD ["/bin/bash", "-c", "ftl $CLONE_CMD $CLONE_URI"]
