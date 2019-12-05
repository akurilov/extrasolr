package com.github.akurilov.extrasolr.fetch;

public interface FetchConfig {

    int CONCURRENCY_LIMIT = 100;
    long CONN_IDLE_TIMEOUT_MILLIS = 10_000;
    long FETCH_PERMIT_WAIT_LIMIT_MILLIS = 10_000;
}
