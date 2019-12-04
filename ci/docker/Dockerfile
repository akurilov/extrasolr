FROM adoptopenjdk:13-jre-hotspot
ARG NATS_VERSION=2.1.2
ARG SOLR_VERSION=8.3.1
RUN apt-get update && \
    apt-get install -y curl lsof unzip && \
    rm -rf /var/lib/apt/lists/* && \
    curl -L https://github.com/nats-io/nats-server/releases/download/v${NATS_VERSION}/nats-server-v${NATS_VERSION}-linux-amd64.zip -o /tmp/nats-server.zip && \
    unzip /tmp/nats-server.zip -d /tmp/nats-server && \
    cp -f /tmp/nats-server/nats-server-v${NATS_VERSION}-linux-amd64/nats-server /usr/local/bin/ && \
    curl -L http://mirror.linux-ia64.org/apache/lucene/solr/${SOLR_VERSION}/solr-${SOLR_VERSION}.tgz -o /tmp/solr.tgz && \
    cd /opt && \
    tar xvf /tmp/solr.tgz && \
    ln -s /opt/solr-${SOLR_VERSION} /opt/solr && \
    rm -rf /tmp/*
COPY build/libs/extrasolr.jar /opt/extrasolr/extrasolr.jar
ADD ci/docker/entrypoint.sh /opt/extrasolr/entrypoint.sh
EXPOSE 4222 8983 9010 9011 9012
ENTRYPOINT /opt/extrasolr/entrypoint.sh
