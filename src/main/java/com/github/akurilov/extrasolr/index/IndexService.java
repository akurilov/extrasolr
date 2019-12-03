package com.github.akurilov.extrasolr.index;

import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.INDEX_HOST;
import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Config.SUBJECT_INDEX;
import static com.github.akurilov.extrasolr.Metrics.outputMetricsLoop;

public final class IndexService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);

    public static void main(final String... args)
    throws Exception {
        final var succCounter = new LongAdder();
        final var failCounter = new LongAdder();
        try(
            final var indexClient = new ConcurrentUpdateSolrClient.Builder("http://" + INDEX_HOST + "/solr/extrasolr")
                .withThreadCount(Runtime.getRuntime().availableProcessors())
                .build()
        ) {
            final var msgHandler = new IndexMessageHandler(indexClient, succCounter, failCounter);
            try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
                mq.subscribe(SUBJECT_INDEX, msgHandler);
                outputMetricsLoop(LOG, 10, succCounter, failCounter);
            }
        }
    }
}
