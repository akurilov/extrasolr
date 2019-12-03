package com.github.akurilov.extrasolr.index;

import com.github.akurilov.extrasolr.mq.MessageHandler;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

import static java.nio.charset.StandardCharsets.UTF_8;

final class IndexMessageHandler
implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(IndexMessageHandler.class);

    private final SolrClient indexClient;
    private final LongAdder succCounter;
    private final LongAdder failCounter;

    IndexMessageHandler(final SolrClient indexClient, final LongAdder succCounter, final LongAdder failCounter) {
        this.indexClient = indexClient;
        this.succCounter = succCounter;
        this.failCounter = failCounter;
    }

    @Override
    public final void accept(final String srcUrl, final byte[] content) {
        final var txt = new String(content, UTF_8);
        final var doc = new SolrInputDocument();
        doc.addField("source", srcUrl);
        doc.addField("content", txt);
        try {
            indexClient.add(doc);
            succCounter.increment();
        } catch(final SolrServerException | IOException e) {
            LOG.warn("Failed to index the document from \"{}\"", srcUrl);
            failCounter.increment();
        }
    }
}
