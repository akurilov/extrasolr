package com.github.akurilov.extrasolr.fetch.netty;

import com.github.akurilov.extrasolr.fetch.TLS;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.akurilov.extrasolr.fetch.TLS.TLS_PROTOCOLS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class NettyHttpChannelInitializer
extends ChannelInitializer<Channel> {

    private final SimpleChannelInboundHandler<? extends HttpObject> respHandler;
    private final boolean tlsFlag;
    private final SslContext tlsCtx;
    private final long idleTimeoutMillis;

    NettyHttpChannelInitializer(
        final SimpleChannelInboundHandler<? extends HttpObject> respHandler, final boolean tlsFlag,
        final long idleTimeoutMillis
    ) {
        this.respHandler = respHandler;
        this.tlsFlag = tlsFlag;
        if(tlsFlag) {
            try {
                final var ciphersAll = SSLContext
                    .getDefault()
                    .getServerSocketFactory()
                    .getSupportedCipherSuites();
                final var ciphers = Arrays
                    .stream(ciphersAll)
                    .filter(TLS::isCipherSuiteAllowed)
                    .collect(Collectors.toList());
                tlsCtx = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .sslProvider(SslProvider.OPENSSL)
                    .protocols(TLS_PROTOCOLS)
                    .ciphers(ciphers)
                    .build();
            } catch(final SSLException | NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
        } else {
            tlsCtx = null;
        }
        this.idleTimeoutMillis = idleTimeoutMillis;
    }

    @Override
    protected final void initChannel(final Channel conn) {
        final var pipeline = conn.pipeline();
        if(tlsFlag) {
            pipeline.addLast(tlsCtx.newHandler(conn.alloc()));
        }
        pipeline
            .addLast(new IdleStateHandler(idleTimeoutMillis, idleTimeoutMillis, idleTimeoutMillis, MILLISECONDS))
            .addLast(new HttpClientCodec())
            .addLast(new HttpContentDecompressor())
            .addLast(respHandler);
    }
}
