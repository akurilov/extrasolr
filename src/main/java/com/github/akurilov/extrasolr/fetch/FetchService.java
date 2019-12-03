package com.github.akurilov.extrasolr.fetch;

import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Config.SUBJECT_FETCH;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class FetchService {

    private static final Logger LOG = LoggerFactory.getLogger(FetchService.class);

    public static void main(final String... args)
    throws Exception {
        final var httpClientConnPool = new ConnectionPool();
        final var httpClient = new OkHttpClient.Builder()
            .connectionPool(httpClientConnPool)
            .build();
        LOG.info("HTTP client initialized");
        try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
            final var respCallback = new HttpResponseCallback(mq);
            mq.subscribe(SUBJECT_FETCH, (none, payload) -> onUri(httpClient, respCallback, payload));
        }
    }

    static void onUri(final OkHttpClient httpClient, final HttpResponseCallback respCallback, final byte[] payload) {
        final var uri = new String(payload, UTF_8);
        final var req = new Request.Builder()
            .get()
            .url(uri)
            .build();
        httpClient
            .newCall(req)
            .enqueue(respCallback);
        LOG.trace("Submitted the HTTP request to: {}", uri);
    }
}
