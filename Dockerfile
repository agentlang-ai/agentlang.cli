FROM clojure:temurin-21-lein-jammy

ENV DOCKER_CONTAINER=Yes

COPY bin/agent /usr/local/bin/
RUN mkdir -p /root/.agentlang/self-installs
COPY target/uberjar/*-standalone.jar /root/.agentlang/self-installs

RUN apt update && apt install -y git wget iproute2
RUN /usr/local/bin/agent

WORKDIR /tmp
RUN git clone https://github.com/agentlang-ai/agentlang.git && cd agentlang && lein install
RUN agent new app hello && cd hello && agent classpath

WORKDIR /agentlang

ENV CLONE_CMD=FIXME
ENV CLONE_URI=FIXME

## See: https://stackoverflow.com/a/58837546
CMD ["/bin/bash", "-c", "agent \"$CLONE_CMD\" \"$CLONE_URI\""]
