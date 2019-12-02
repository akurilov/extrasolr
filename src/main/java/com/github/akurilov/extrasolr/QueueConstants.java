package com.github.akurilov.extrasolr;

public interface QueueConstants {

    String QUEUE_URI_SCHEMA = "nats://";
    String[] QUEUE_HOSTS = new String[] {
        "localhost:4222",
    };
    String QUEUE_URL = "url";
    String QUEUE_CONTENT = "content";
}
