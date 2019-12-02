package com.github.akurilov.extrasolr;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.util.Arrays;

import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_CONTENT;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_URI_SCHEMA;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ContentService {

    public static void main(final String... args)
    throws Exception {
        final var natsOptsBuilder = new Options.Builder();
        Arrays
            .stream(QUEUE_HOSTS)
            .map(host -> QUEUE_URI_SCHEMA + host)
            .forEach(natsOptsBuilder::server);
        final var natsOpts = natsOptsBuilder.build();
        try(final var natsConn = Nats.connect(natsOpts)) {
            final var dispatcher = natsConn.createDispatcher((msg) -> onContent(natsConn, msg));
            try {
                dispatcher.subscribe(QUEUE_CONTENT);
                Thread.currentThread().join();
            } finally {
                dispatcher.unsubscribe(QUEUE_CONTENT);
                natsConn.closeDispatcher(dispatcher);
            }
        }
    }

    static void onContent(final Connection natsConn, final Message msg) {
        final var content = new String(msg.getData(), UTF_8);
    }
}
