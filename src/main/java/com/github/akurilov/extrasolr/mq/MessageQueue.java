package com.github.akurilov.extrasolr.mq;

public interface MessageQueue
extends AutoCloseable {

    void publish(final String subject, final String metadata, final byte[] payload);

    void subscribe(final String subject, final MessageHandler msgHandler);

    long payloadSizeLimit();
}
