#!/bin/sh

APP_JAR_PATH=/opt/extrasolr/extrasolr.jar
APP_JAVA_ROOT_PKG=com.github.akurilov.extrasolr
APP_JMX_OPTS_AND_PORT="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port"
APP_JAVA_OPT_HEAP_DUMP_ON_OOM="-XX:+HeapDumpOnOutOfMemoryError"
SOLR_CMD=/opt/solr/bin/solr

# run the nats server
nohup nats-server &

# run solr
${SOLR_CMD} start -force
${SOLR_CMD} create -c extrasolr -force

# run the services
nohup java -Xms256m -Xmx256m ${APP_JAVA_OPT_HEAP_DUMP_ON_OOM} "${APP_JMX_OPTS_AND_PORT}"=9010 -cp ${APP_JAR_PATH} ${APP_JAVA_ROOT_PKG}.index.IndexService &
nohup java -Xms512m -Xmx512m ${APP_JAVA_OPT_HEAP_DUMP_ON_OOM} "${APP_JMX_OPTS_AND_PORT}"=9011 -cp ${APP_JAR_PATH} ${APP_JAVA_ROOT_PKG}.parse.ParseService &
java -Xms1280m -Xmx1280m "${APP_JMX_OPTS_AND_PORT}"=9012 -cp ${APP_JAR_PATH} ${APP_JAVA_ROOT_PKG}.fetch.FetchService
