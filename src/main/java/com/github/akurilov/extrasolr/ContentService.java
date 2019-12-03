package com.github.akurilov.extrasolr;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_CONTENT;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_URI_SCHEMA;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ContentService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentService.class);
    private static final String GROUP_NAME_VALUE = "value";
    private static final Pattern PATTERN_HREF_VALUE = Pattern.compile(
        "href=\"(?<" + GROUP_NAME_VALUE + ">http(s)?://[\\S^\"]{8,256})\""
    );


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
        LOG.info("Consuming the content message of {} bytes from: {}", content.length());
    }

    static void extractAndPublishLinks(final @NotNull String text) {
        final var matcher = PATTERN_HREF_VALUE.matcher(text);
        while(matcher.find()) {
            final var url = matcher.group(GROUP_NAME_VALUE);

        }
    }
}
