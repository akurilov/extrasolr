package com.github.akurilov.extrasolr.mq;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface MessageHandler
extends BiConsumer<String, byte[]> {
}
