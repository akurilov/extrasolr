package com.github.akurilov.extrasolr.fetch;

import com.github.akurilov.extrasolr.fetch.netty.NettyHttpClientImpl;
import com.github.akurilov.extrasolr.mq.MessageHandler;
import com.github.akurilov.extrasolr.mq.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.LongAdder;
import static java.nio.charset.StandardCharsets.UTF_8;

final class FetchUriMessageHandler
implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FetchUriMessageHandler.class);

    private final LongAdder failCounter;
    private FetchClient client;

    public FetchUriMessageHandler(final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter) {
        this.failCounter = failCounter;
        this.client = new NettyHttpClientImpl(mq, succCounter, failCounter);
    }

    @Override
    public void accept(final String ignored, final byte[] uriBytes) {
        final var uriRaw = new String(uriBytes, UTF_8);
        LOG.trace("Got URI to fetch: {}", uriRaw);
        try {
            final var uri = new URI(uriRaw);
            client.fetch(uri);
        } catch(final URISyntaxException e) {
            LOG.debug("Failed to parse the uri \"" + uriRaw + "\"", e);
            failCounter.increment();
        }
    }
}
