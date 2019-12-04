package com.github.akurilov.extrasolr.fetch.netty;

import com.github.akurilov.extrasolr.mq.MessageQueue;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpStatusClass;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.SUBJECT_PARSE;
import static com.github.akurilov.extrasolr.fetch.netty.Constants.ATTR_KEY_URI;
import static java.nio.charset.StandardCharsets.UTF_8;

@ChannelHandler.Sharable
public final class HttpResponseHandler
extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandler.class);

    private final MessageQueue mq;
    private final LongAdder succCounter;
    private final LongAdder failCounter;

    public HttpResponseHandler(final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter) {
        this.mq = mq;
        this.succCounter = succCounter;
        this.failCounter = failCounter;
    }

    @Override
    protected final void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) {
        LOG.info("HTTP response: {}", msg);
        if(msg instanceof FullHttpResponse) {
            final var resp = (FullHttpResponse) msg;
            final var status = resp.status();
            if(HttpStatusClass.SUCCESS.contains(status.code())) {
                final var contentType = resp.headers().get(HttpHeaderNames.CONTENT_TYPE);
                if(null == contentType) {
                    LOG.warn("Failed to determine the response content type");
                    failCounter.increment();
                } else {
                    if(contentType.startsWith("text")) {
                        final var mediaType = MediaType.parse(contentType);
                        final var content = resp.content();
                        final var uri = ctx.channel().attr(ATTR_KEY_URI).get().toString();
                        final var charset = mediaType == null ? UTF_8 : mediaType.charset(UTF_8);
                        final byte[] contentBytes;
                        if(UTF_8.equals(charset)) {
                            if(content.hasArray()) {
                                contentBytes = content.array();
                            } else {
                                contentBytes = new byte[content.readableBytes()];
                                content.getBytes(0, contentBytes);
                            }
                        } else {
                            contentBytes = content.toString(charset).getBytes(UTF_8);
                        }
                        mq.publish(SUBJECT_PARSE, uri, contentBytes);
                        succCounter.increment();
                    } else {
                        LOG.debug("Unsupported response content type \"{}\"", contentType);
                        failCounter.increment();
                    }
                }
            } else {
                LOG.debug("Unsuccessful HTTP response: {}", status);
                failCounter.increment();
            }
        } else {
            throw new AssertionError("Full HTTP response is expected, got " + msg.getClass());
        }
    }

    @Override
    public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.warn("HTTP response handler failure", cause);
        ctx.close();
        failCounter.increment();
    }
}
