FROM clojure:temurin-21-lein-jammy

ENV DOCKER_CONTAINER=Yes

RUN apt update && apt install -y git wget iproute2

WORKDIR /tmp
ARG CACHE_BUST
RUN git clone https://github.com/agentlang-ai/agentlang.git && cd agentlang && lein install

COPY bin/agent /usr/local/bin/
RUN mkdir -p /root/.agentlang/self-installs
COPY target/uberjar/*-standalone.jar /root/.agentlang/self-installs

WORKDIR /agentlang

COPY resource/download-libs.sh /agentlang/download-libs.sh
RUN ./download-libs.sh
RUN rm download-libs.sh

ENV CLONE_CMD=FIXME
ENV CLONE_URI=FIXME

## See: https://stackoverflow.com/a/58837546
CMD ["/bin/bash", "-c", "agent \"$CLONE_CMD\" \"$CLONE_URI\""]
