package com.github.akurilov.extrasolr.fetch;

import java.net.URI;

public interface FetchClient
extends AutoCloseable {

    void fetch(final URI uri);
}
