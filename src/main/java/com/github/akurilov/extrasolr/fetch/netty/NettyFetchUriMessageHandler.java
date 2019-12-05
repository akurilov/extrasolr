package com.github.akurilov.extrasolr.fetch.netty;

import com.github.akurilov.extrasolr.mq.MessageHandler;
import com.github.akurilov.extrasolr.mq.MessageQueue;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.fetch.netty.Constants.ATTR_KEY_URI;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.channel.ChannelOption.TCP_NODELAY;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class NettyFetchUriMessageHandler
implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NettyFetchUriMessageHandler.class);

    private final Bootstrap bootstrap;
    private final LongAdder failCounter;

    public NettyFetchUriMessageHandler(
        final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter
    ) {
        final var respHandler = new NettyHttpResponseHandler(mq, succCounter, failCounter);
        final long mqPayloadSizeLimit = mq.payloadSizeLimit();
        final int payloadSizeLimit = mqPayloadSizeLimit > Integer.MAX_VALUE ?
            Integer.MAX_VALUE : (int) mqPayloadSizeLimit;
        bootstrap = new Bootstrap()
            .group(new io.netty.channel.epoll.EpollEventLoopGroup())
            .channel(io.netty.channel.epoll.EpollSocketChannel.class)
            .option(TCP_NODELAY, true)
            .handler(new NettyHttpChannelInitializer(respHandler, payloadSizeLimit, true));
        this.failCounter = failCounter;
    }

    @Override
    public void accept(final String ignored, final byte[] uriBytes) {
        final var uriRaw = new String(uriBytes, UTF_8);
        LOG.trace("Got URI to fetch: {}", uriRaw);
        try {
            final var uri = new URI(uriRaw);
            final var protocol = uri.getScheme();
            int port = uri.getPort();
            if(port <= 0) {
                if("https".equals(protocol)) {
                    port = 443;
                } else {
                    port = 80;
                }
            }
            final var connFuture = bootstrap.connect(uri.getHost(), port);
            connFuture.addListener((v) -> onUriConnection(connFuture.channel(), uri));
        } catch(final URISyntaxException e) {
            LOG.debug("Failed to parse the uri \"" + uriRaw + "\"", e);
            failCounter.increment();
        }
    }

    static void onUriConnection(final Channel conn, final URI uri) {
        conn.attr(ATTR_KEY_URI).set(uri);
        final var req = new DefaultFullHttpRequest(HTTP_1_1, GET, uri.getRawPath(), EMPTY_BUFFER);
        final var headers = req.headers();
        headers.set(HttpHeaderNames.HOST, uri.getHost());
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        conn.writeAndFlush(req);
    }
}
