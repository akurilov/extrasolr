#!/bin/sh
umask 0000
# run the nats server
nohup nats-server &
# run solr
/opt/solr/bin/solr start -force
/opt/solr/bin/solr create -c extrasolr -force
# run the application
nohup java -cp /opt/extrasolr/extrasolr.jar com.github.akurilov.extrasolr.UrlService &
java -cp /opt/extrasolr/extrasolr.jar com.github.akurilov.extrasolr.ContentService
