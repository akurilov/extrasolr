package com.github.akurilov.extrasolr.index;

import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import com.github.akurilov.extrasolr.parse.ParseService;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.akurilov.extrasolr.Config.INDEX_HOST;
import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Config.SUBJECT_INDEX;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class IndexService {

    private static final Logger LOG = LoggerFactory.getLogger(ParseService.class);

    public static void main(final String... args)
    throws Exception {
        final var indexClient = new ConcurrentUpdateSolrClient.Builder(INDEX_HOST)
            .withThreadCount(Runtime.getRuntime().availableProcessors())
            .build();
        try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
            mq.subscribe(SUBJECT_INDEX, (srcUrl, content) -> onContent(indexClient, srcUrl, content));

            Thread.currentThread().join();
        }
    }

    static void onContent(final SolrClient indexClient, final String srcUrl, final byte[] content) {
        final var txt = new String(content, UTF_8);
        final var doc = new SolrInputDocument();
        doc.addField("source", srcUrl);
        doc.
        System.out.println(
            "================================================================================================================================\n" +
            srcUrl + "\n" +
            "--------------------------------------------------------------------------------------------------------------------------------\n" +
            txt
        );
    }
}
