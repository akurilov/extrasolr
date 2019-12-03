package com.github.akurilov.extrasolr.fetch;

import com.github.akurilov.extrasolr.mq.MessageQueue;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.SUBJECT_PARSE;
import static java.nio.charset.StandardCharsets.UTF_8;

final class HttpResponseCallback
implements Callback {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseCallback.class);

    private final MessageQueue mq;
    private final LongAdder succCounter;
    private final LongAdder failCounter;
    private final int msgPayloadSizeLimit;

    HttpResponseCallback(final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter) {
        this.mq = mq;
        this.succCounter = succCounter;
        this.failCounter = failCounter;
        this.msgPayloadSizeLimit = mq.payloadSizeLimit() > Integer.MAX_VALUE ?
            Integer.MAX_VALUE :
            (int) mq.payloadSizeLimit();
        LOG.info("Message queue max payload size: {}", this.msgPayloadSizeLimit);
    }

    @Override
    public final void onFailure(@NotNull final Call call, @NotNull final IOException e) {
        LOG.debug(call.request().url() + ": request failure", e);
        failCounter.increment();
    }

    @Override
    public final void onResponse(@NotNull final Call call, @NotNull final Response response)
    throws IOException {
        final var reqUrl = call.request().url().toString();
        try(final var body = response.body()) {
            if(null == body) {
                LOG.warn("{}: no response body", reqUrl);
                failCounter.increment();
            } else {
                var contentLength = (int) body.contentLength();
                contentLength = contentLength < 0 || contentLength > msgPayloadSizeLimit ?
                    msgPayloadSizeLimit :
                    contentLength;
                if(contentLength == 0) {
                    LOG.warn("{}: content length is {}", reqUrl, contentLength);
                    failCounter.increment();
                } else {
                    final var contentType = body.contentType();
                    if(null == contentType) {
                        LOG.warn("{}: failed to determine the content type", reqUrl);
                        failCounter.increment();
                    } else {
                        final var contentTypePrefix = contentType.type();
                        if(contentTypePrefix.equals("text")) {
                            final byte[] buff = new byte[contentLength];
                            var byteCount = 0;
                            try(final var in = body.byteStream()) {
                                int n;
                                while(0 < (n = in.read(buff, byteCount, contentLength - byteCount))) {
                                    byteCount += n;
                                }
                            }
                            final var charset = contentType.charset(UTF_8);
                            if(null == charset) {
                                throw new AssertionError("Null charset is not expected");
                            }
                            final var text = new String(buff, 0, byteCount, charset);
                            mq.publish(SUBJECT_PARSE, reqUrl, text.getBytes(UTF_8));
                            succCounter.increment();
                        } else {
                            LOG.debug("{}: unsupported content type \"{}\"", contentType, reqUrl);
                            failCounter.increment();
                        }
                    }
                }
            }
        }
    }
}
