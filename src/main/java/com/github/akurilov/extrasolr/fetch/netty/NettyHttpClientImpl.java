package com.github.akurilov.extrasolr.fetch.netty;

import com.github.akurilov.extrasolr.fetch.FetchClient;
import com.github.akurilov.extrasolr.mq.MessageQueue;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.akurilov.extrasolr.fetch.FetchConfig.CONCURRENCY_LIMIT;
import static com.github.akurilov.extrasolr.fetch.FetchConfig.CONN_IDLE_TIMEOUT_MILLIS;
import static com.github.akurilov.extrasolr.fetch.FetchConfig.FETCH_PERMIT_WAIT_LIMIT_MILLIS;
import static com.github.akurilov.extrasolr.fetch.netty.Constants.ATTR_KEY_URI;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.channel.ChannelOption.TCP_NODELAY;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class NettyHttpClientImpl
implements FetchClient {

    private static final Logger LOG = LoggerFactory.getLogger(NettyHttpClientImpl.class);

    private final Lock clientInitLock = new ReentrantLock();
    private final MessageQueue mq;
    private final LongAdder succCounter;
    private final LongAdder failCounter;

    private volatile Bootstrap bootstrap;
    private volatile EventLoopGroup evtLoopGroup;
    private volatile Semaphore concurrencyThrottle;
    private volatile long clientInitTime;

    public NettyHttpClientImpl(final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter) {
        init(mq, succCounter, failCounter);
        this.mq = mq;
        this.succCounter = succCounter;
        this.failCounter = failCounter;
    }

    private void init(final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter) {
        this.concurrencyThrottle = new Semaphore(CONCURRENCY_LIMIT);
        final var respHandler = new NettyHttpResponseHandler(mq, concurrencyThrottle, succCounter, failCounter);
        this.evtLoopGroup = new EpollEventLoopGroup();
        this.bootstrap = new Bootstrap()
            .group(evtLoopGroup)
            .channel(io.netty.channel.epoll.EpollSocketChannel.class)
            .option(TCP_NODELAY, true)
            .handler(new NettyHttpChannelInitializer(respHandler, true, CONN_IDLE_TIMEOUT_MILLIS));
        this.clientInitTime = currentTimeMillis();
    }

    @Override
    public final void fetch(final URI uri) {
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
    }

    @Override
    public final void close() {
        evtLoopGroup.shutdownGracefully(1, 1, MILLISECONDS);
    }

    final void onUriConnection(final Channel conn, final URI uri)
    throws InterruptedException {
        conn.attr(ATTR_KEY_URI).set(uri);
        final var req = new DefaultFullHttpRequest(HTTP_1_1, GET, uri.getRawPath(), EMPTY_BUFFER);
        final var headers = req.headers();
        headers.set(HttpHeaderNames.HOST, uri.getHost());
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        if(!concurrencyThrottle.tryAcquire(FETCH_PERMIT_WAIT_LIMIT_MILLIS, MILLISECONDS)) {
            clientInitLock.lock();
            try {
                if(currentTimeMillis() - clientInitTime > FETCH_PERMIT_WAIT_LIMIT_MILLIS) {
                    LOG.warn("Re init the HTTP client");
                    close();
                    init(mq, succCounter, failCounter);
                }
            } finally {
                clientInitLock.unlock();
            }
        }
        conn.writeAndFlush(req);
    }
}
