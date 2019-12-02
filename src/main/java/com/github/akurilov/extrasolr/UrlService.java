package com.github.akurilov.extrasolr;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_CONTENT;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_URI_SCHEMA;
import static com.github.akurilov.extrasolr.QueueConstants.QUEUE_URL;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class UrlService {

    public static void main(final String... args)
    throws Exception {
        final var httpClientConnPool = new ConnectionPool();
        final var httpClient = new OkHttpClient.Builder()
            .connectionPool(httpClientConnPool)
            .build();
        final var natsOptsBuilder = new Options.Builder();
        Arrays
            .stream(QUEUE_HOSTS)
            .map(host -> QUEUE_URI_SCHEMA + host)
            .forEach(natsOptsBuilder::server);
        final var natsOpts = natsOptsBuilder.build();
        try(final var natsConn = Nats.connect(natsOpts)) {

            final var respCallback = new ResponseCallback(natsConn);
            final var dispatcher = natsConn.createDispatcher((msg) -> onUri(httpClient, respCallback, msg));
            try {
                dispatcher.subscribe(QUEUE_URL);
                Thread.currentThread().join();
            } finally {
                dispatcher.unsubscribe(QUEUE_URL);
                natsConn.closeDispatcher(dispatcher);
            }
        }
    }

    static void onUri(final OkHttpClient httpClient, final ResponseCallback respCallback, final Message msg) {
        final var data = msg.getData();
        final var uri = new String(data, UTF_8);
        final var req = new Request.Builder()
            .get()
            .url(uri)
            .build();
        httpClient
            .newCall(req)
            .enqueue(respCallback);
    }

    static final class ResponseCallback
    implements Callback {

        private final Connection natsConn;
        private final long msgPayloadSizeLimit;

        ResponseCallback(final Connection natsConn) {
            this.natsConn = natsConn;
            this.msgPayloadSizeLimit = natsConn.getMaxPayload();
        }

        @Override
        public final void onFailure(@NotNull final Call call, @NotNull final IOException e) {
            e.printStackTrace(System.err);
        }

        @Override
        public final void onResponse(@NotNull final Call call, @NotNull final Response response)
        throws IOException {
            try(final var body = response.body()) {
                if(null != body) {
                    final long contentLength = body.contentLength();
                    if(contentLength > 0) {
                        final var actualContentLength = (int) min(contentLength, msgPayloadSizeLimit);
                        final var contentType = body.contentType();
                        if(null != contentType) {
                            final var contentTypePrefix = contentType.type();
                            if(contentTypePrefix.equals("text")) {
                                final byte[] buff = new byte[actualContentLength];
                                var byteCount = 0;
                                try(final var in = body.byteStream()) {
                                    int n;
                                    while(0 < (n = in.read(buff, byteCount, actualContentLength - byteCount))) {
                                        byteCount += n;
                                    }
                                }
                                final var charset = contentType.charset(UTF_8);
                                if(null == charset) {
                                    throw new AssertionError("Null charset is not expected");
                                }
                                final var text = new String(buff, 0, byteCount, charset);
                                natsConn.publish(QUEUE_CONTENT, text.getBytes(UTF_8));
                            }
                        }
                    }
                }
            }
        }
    }
}
