package com.github.akurilov.extrasolr.parse;

import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

import static com.github.akurilov.extrasolr.Config.SUBJECT_PARSE;
import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Metrics.outputMetricsLoop;

public class ParseService {

    private static final Logger LOG = LoggerFactory.getLogger(ParseService.class);

    public static void main(final String... args)
    throws Exception {
        final var succCounter = new LongAdder();
        final var failCounter = new LongAdder();
        try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
            final var contentHandler = new ParseMessageHandler(mq, succCounter, failCounter);
            mq.subscribe(SUBJECT_PARSE, contentHandler);
            outputMetricsLoop(LOG, 10, succCounter, failCounter);
        }
    }
}
