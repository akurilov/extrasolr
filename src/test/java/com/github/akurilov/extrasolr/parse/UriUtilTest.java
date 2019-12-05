package com.github.akurilov.extrasolr.parse;

import org.junit.jupiter.api.Test;

import static com.github.akurilov.extrasolr.parse.UriUtil.toAbsoluteUri;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UriUtilTest {

    @Test
    public void testToAbsoluteUri1()
    throws Exception {
        final var srcUri = "https://example.com";
        final var link = "/foo/bar";
        assertEquals("https://example.com/foo/bar", toAbsoluteUri(srcUri, link));
    }

    @Test
    public void testToAbsoluteUri2()
    throws Exception {
        final var srcUri = "https://example.com/";
        final var link = "/foo/";
        assertEquals("https://example.com/foo/", toAbsoluteUri(srcUri, link));
    }

    @Test
    public void testToAbsoluteUri3()
    throws Exception {
        final var srcUri = "example.com:1234";
        final var link = "/";
        assertEquals("example.com:1234/", toAbsoluteUri(srcUri, link));
    }

    @Test
    public void testToAbsoluteUri4()
    throws Exception {
        final var srcUri = "https://example.com";
        final var link = "http://foo.bar/123";
        assertEquals("http://foo.bar/123", toAbsoluteUri(srcUri, link));
    }
}
