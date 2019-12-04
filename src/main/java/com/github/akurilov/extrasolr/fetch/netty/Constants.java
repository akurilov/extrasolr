package com.github.akurilov.extrasolr.fetch.netty;

import io.netty.util.AttributeKey;

import java.net.URI;

public interface Constants {

    AttributeKey<URI> ATTR_KEY_URI = AttributeKey.valueOf("uri");
}
