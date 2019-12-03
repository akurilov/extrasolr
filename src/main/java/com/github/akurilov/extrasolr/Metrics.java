package com.github.akurilov.extrasolr;

import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public interface Metrics {

    static void outputMetricsLoop(
        final Logger log, final int periodSec, final LongAdder succCounter, final LongAdder failCounter
    ) throws InterruptedException {
        if(periodSec <= 0) {
            throw new IllegalArgumentException();
        }
        var lastSuccCount = 0L;
        while(true) {
            final var succCount = succCounter.sum();
            final var rate = ((double) (succCount - lastSuccCount)) / periodSec;
            lastSuccCount = succCount;
            log.info("count success={}, failed={}, rate={} [op/s]", succCount, failCounter.sum(), rate);
            TimeUnit.SECONDS.sleep(periodSec);
        }
    }
}
