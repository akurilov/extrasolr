package com.github.akurilov.extrasolr;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class UrlService {

    public static void main(final String... args) {
        final var connPool = new ConnectionPool();
        final var httpClient = new OkHttpClient.Builder()
            .connectionPool(connPool)
            .build();
        try(final var ctx = new ZContext()) {
            try(
                final var uriQueue = ZmqSocket
                    .builder()
                    .context(ctx)
                    .socketType(SocketType.SUB)
                    .host(QueueConstants.HOST)
                    .port(QueueConstants.PORT)
                    .topic(QueueConstants.URL)
                    .build()
            ) {
                try(
                    final var contentQueue = ZmqSocket
                        .builder()
                        .context(ctx)
                        .socketType(SocketType.PUB)
                        .host(QueueConstants.HOST)
                        .port(QueueConstants.PORT)
                        .topic(QueueConstants.CONTENT)
                        .build()
                ) {
                    final var respCallback = new ResponseCallback(contentQueue);
                    while(!Thread.currentThread().isInterrupted()) {
                        download(httpClient, uriQueue, respCallback);
                    }
                }
            }
        }
    }

    static final class ResponseCallback
    implements Callback {

        static int CONTENT_LENGTH_LIMIT = 0x100000; // 1 MiB

        private final ZMQ.Socket contentQueue;

        ResponseCallback(final ZMQ.Socket contentQueue) {
            this.contentQueue = contentQueue;
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
                        final var actualContentLength = (int) min(contentLength, CONTENT_LENGTH_LIMIT);
                        final var contentType = body.contentType();
                        if(null != contentType) {
                            final var contentTypePrefix = contentType.type();
                            if(contentTypePrefix.equals("text")) {
                                final byte[] buff = new byte[actualContentLength];
                                try(final var in = body.byteStream()) {
                                    var doneByteCount = 0;
                                    int n;
                                    while(0 < (n = in.read(buff, doneByteCount, actualContentLength - doneByteCount))) {
                                        doneByteCount += n;
                                    }
                                }
                                final var charset = contentType.charset(UTF_8);
                                if(null == charset) {
                                    throw new AssertionError("Null charset is not expected");
                                }
                                final var text = new String(buff, charset);
                                if(!contentQueue.send(text)) {
                                    throw new AssertionError("Failed to enqueue the content");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static void download(final OkHttpClient httpClient, final ZMQ.Socket uriQueue, final Callback respCallback) {
        final var uri = uriQueue.recvStr(ZMQ.DONTWAIT);
        if(null == uri) {
            LockSupport.parkNanos(1); // notify the scheduler that there's nothing to do
        } else {
            final var req = new Request.Builder()
                .get()
                .url(uri)
                .build();
            httpClient
                .newCall(req)
                .enqueue(respCallback);
        }
    }
}
