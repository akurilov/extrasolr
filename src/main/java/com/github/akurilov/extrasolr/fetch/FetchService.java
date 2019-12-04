package com.github.akurilov.extrasolr.fetch;

import com.github.akurilov.extrasolr.fetch.netty.HttpChannelInitializer;
import com.github.akurilov.extrasolr.fetch.netty.HttpResponseHandler;
import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Config.SUBJECT_FETCH;
import static com.github.akurilov.extrasolr.Metrics.outputMetricsLoop;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.channel.ChannelOption.TCP_NODELAY;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class FetchService {

    private static final Logger LOG = LoggerFactory.getLogger(FetchService.class);

    public static void main(final String... args)
    throws Exception {
        final var succCounter = new LongAdder();
        final var failCounter = new LongAdder();
        try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
            final long mqPayloadSizeLimit = mq.payloadSizeLimit();
            final int payloadSizeLimit = mqPayloadSizeLimit > Integer.MAX_VALUE ?
                Integer.MAX_VALUE : (int) mqPayloadSizeLimit;
            final var respHandler = new HttpResponseHandler(mq, succCounter, failCounter);
            final var bootstrap = new Bootstrap()
                .group(new io.netty.channel.epoll.EpollEventLoopGroup())
                .channel(io.netty.channel.epoll.EpollSocketChannel.class)
                .option(TCP_NODELAY, true)
                .handler(new HttpChannelInitializer(respHandler, payloadSizeLimit, true));
            mq.subscribe(SUBJECT_FETCH, (none, payload) -> onUri(bootstrap, failCounter, payload));
            outputMetricsLoop(LOG, 10, succCounter, failCounter);
        }
    }

    static void onUri(final Bootstrap bootstrap, final LongAdder failCounter, final byte[] payload) {
        final var uriRaw = new String(payload, UTF_8);
        LOG.trace("Got URI to fetch: {}", uriRaw);
        try {
            final var uri = new URI(uriRaw);
            final var port = uri.getPort() > 0 ? uri.getPort() : 80;
            final var connFuture = bootstrap.connect(uri.getHost(), port);
            connFuture.addListener((ignored) -> onUriConnection(connFuture.channel(), uri));
        } catch(final URISyntaxException e) {
            LOG.warn("Failed to parse the uri \"" + uriRaw + "\"", e);
            failCounter.increment();
        }
    }

    static void onUriConnection(final Channel conn, final URI uri) {
        final var req = new DefaultFullHttpRequest(HTTP_1_0, GET, uri.getRawPath(), EMPTY_BUFFER);
        conn.writeAndFlush(req);
        LOG.trace("Submitted the HTTP request to: {}", uri);
        conn.close();
    }
}
