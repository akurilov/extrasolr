package com.github.akurilov.extrasolr.mq.nats;

import com.github.akurilov.extrasolr.mq.MessageHandler;
import com.github.akurilov.extrasolr.mq.MessageQueue;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public final class NatsMessageQueue
implements MessageQueue {

    private static final Logger LOG = LoggerFactory.getLogger(NatsMessageQueue.class);

    private static final String QUEUE_URI_SCHEMA = "nats://";
    private final Connection conn;
    private final long payloadSizeLimit;

    public NatsMessageQueue(final String[] hosts)
    throws IOException, InterruptedException {
        final var natsOptsBuilder = new Options.Builder();
        Arrays
            .stream(hosts)
            .map(host -> QUEUE_URI_SCHEMA + host)
            .forEach(natsOptsBuilder::server);
        final var natsOpts = natsOptsBuilder.build();
        this.conn = Nats.connect(natsOpts);
        LOG.info("Connected to the message queue");
        this.payloadSizeLimit = conn.getMaxPayload();
        LOG.info("Payload size limit: {}", payloadSizeLimit);
    }

    @Override
    public final void publish(final String subject, final String metadata, final byte[] payload) {
        conn.publish(subject, metadata, payload);
        LOG.trace("Publish: subject \"{}\", metadata \"{}\", {} bytes of payload", subject, metadata, payload.length);
    }

    @Override
    public final void subscribe(final String subject, final MessageHandler msgHandler) {
        final var dispatcher = conn.createDispatcher(
            (msg) -> {
                final var metadata = msg.getReplyTo();
                final var payload = msg.getData();
                LOG.trace(
                    "Consume: subject \"{}\", metadata \"{}\", {} bytes of payload", subject, metadata, payload.length
                );
                msgHandler.accept(metadata, payload);
            }
        );
        dispatcher.subscribe(subject);
        LOG.info("Subscribed to the subject \"{}\"", subject);
    }

    @Override
    public final long payloadSizeLimit() {
        return payloadSizeLimit;
    }

    @Override
    public final void close()
    throws Exception {
        conn.close();
    }
}
