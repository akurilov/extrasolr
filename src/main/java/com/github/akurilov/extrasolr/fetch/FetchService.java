package com.github.akurilov.extrasolr.fetch;

import com.github.akurilov.extrasolr.fetch.netty.NettyFetchUriMessageHandler;
import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Config.SUBJECT_FETCH;
import static com.github.akurilov.extrasolr.Metrics.outputMetricsLoop;

public final class FetchService {

    private static final Logger LOG = LoggerFactory.getLogger(FetchService.class);

    public static void main(final String... args)
    throws Exception {
        final var succCounter = new LongAdder();
        final var failCounter = new LongAdder();
        try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
            final var fetchClient = new NettyFetchUriMessageHandler(mq, succCounter, failCounter);
            mq.subscribe(SUBJECT_FETCH, fetchClient);
            outputMetricsLoop(LOG, 10, succCounter, failCounter);
        }
    }
}
