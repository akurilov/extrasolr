package com.github.akurilov.extrasolr.fetch.netty;

import io.netty.util.AttributeKey;

import java.net.URI;
import java.nio.charset.Charset;

public interface Constants {

    AttributeKey<URI> ATTR_KEY_URI = AttributeKey.valueOf("uri");
    AttributeKey<Charset> ATTR_KEY_CHARSET = AttributeKey.valueOf("charset");
}
