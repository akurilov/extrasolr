#!/bin/sh
# run the nats server
nohup nats-server &
# run solr
/opt/solr/bin/solr start -force
/opt/solr/bin/solr create -c extrasolr -force
# run the services
nohup java -cp /opt/extrasolr/extrasolr.jar com.github.akurilov.extrasolr.index.IndexService &
nohup java -cp /opt/extrasolr/extrasolr.jar com.github.akurilov.extrasolr.parse.ParseService &
java -cp /opt/extrasolr/extrasolr.jar com.github.akurilov.extrasolr.fetch.FetchService
