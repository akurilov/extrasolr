package com.github.akurilov.extrasolr.fetch.netty;

import com.github.akurilov.extrasolr.mq.MessageQueue;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleStateEvent;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.SUBJECT_PARSE;
import static com.github.akurilov.extrasolr.fetch.netty.Constants.ATTR_KEY_CHARSET;
import static com.github.akurilov.extrasolr.fetch.netty.Constants.ATTR_KEY_URI;
import static java.nio.charset.StandardCharsets.UTF_8;

@ChannelHandler.Sharable
final class NettyHttpResponseHandler
extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger LOG = LoggerFactory.getLogger(NettyHttpResponseHandler.class);

    private final MessageQueue mq;
    private final Semaphore concurrencyThrottle;
    private final LongAdder succCounter;
    private final LongAdder failCounter;
    private final int contentLengthLimit;

    NettyHttpResponseHandler(
        final MessageQueue mq, final Semaphore concurrencyThrottle, final LongAdder succCounter,
        final LongAdder failCounter
    ) {
        this.mq = mq;
        this.concurrencyThrottle = concurrencyThrottle;
        this.succCounter = succCounter;
        this.failCounter = failCounter;
        this.contentLengthLimit = mq.payloadSizeLimit() > Integer.MAX_VALUE ?
            Integer.MAX_VALUE : (int) mq.payloadSizeLimit();
    }

    @Override
    protected final void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) {
        final var conn = ctx.channel();
        if(msg instanceof HttpResponse) {
            LOG.trace("HTTP response: {}", msg);
            final var resp = (HttpResponse) msg;
            final var status = resp.status();
            if(!HttpStatusClass.SUCCESS.contains(status.code())) {
                LOG.warn("Response status: {}", status);
                complete(ctx, failCounter);
            }
            final var contentType = resp.headers().get(HttpHeaderNames.CONTENT_TYPE);
            if(null == contentType) {
                LOG.warn("Failed to determine the response content type");
                complete(ctx, failCounter);
            } else {
                if(contentType.startsWith("text")) {
                    final var mediaType = MediaType.parse(contentType);
                    final var charset = mediaType == null ? UTF_8 : mediaType.charset(UTF_8);
                    if(msg instanceof FullHttpResponse) {
                        final var fullResp = (FullHttpResponse) msg;
                        final var uri = conn.attr(ATTR_KEY_URI).get().toString();
                        handleContent(uri, charset, fullResp.content());
                        complete(ctx, succCounter);
                    } else {
                        conn.attr(ATTR_KEY_CHARSET).set(charset);
                    }
                } else {
                    LOG.warn("Unsupported response content content type: {}", contentType);
                    complete(ctx, failCounter);
                }
            }
        }
        if(msg instanceof HttpContent) {
            handleContent(conn, ((HttpContent) msg).content());
            if(msg instanceof LastHttpContent) {
                complete(ctx, succCounter);
            }
        }
    }

    final void handleContent(final Channel conn, final ByteBuf content) {
        final var uri = conn.attr(ATTR_KEY_URI).get().toString();
        final var charset = conn.attr(ATTR_KEY_CHARSET).get();
        handleContent(uri, charset, content);
    }

    final void handleContent(final String uri, final Charset charset, final ByteBuf content) {
        final var extraContentLength = content.readableBytes() - contentLengthLimit;
        if(extraContentLength > 0) {
            content.readerIndex(content.readerIndex() - extraContentLength);
        }
        final byte[] contentBytes;
        if(UTF_8.equals(charset)) {
            contentBytes = new byte[content.readableBytes()];
            content.getBytes(0, contentBytes);
        } else {
            contentBytes = content.toString(charset).getBytes(UTF_8);
        }
        mq.publish(SUBJECT_PARSE, uri, contentBytes);
    }

    @Override
    public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.warn("HTTP response handler failure", cause);
        complete(ctx, failCounter);
    }

    @Override
    public final void userEventTriggered(final ChannelHandlerContext ctx, final Object evt)
    throws Exception {
        if (evt instanceof IdleStateEvent) {
            throw new SocketTimeoutException();
        }
    }

    private void complete(final ChannelHandlerContext ctx, final LongAdder counter) {
        concurrencyThrottle.release();
        ctx.close();
        counter.increment();
    }
}
