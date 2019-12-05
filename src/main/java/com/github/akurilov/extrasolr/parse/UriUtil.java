package com.github.akurilov.extrasolr.parse;

interface UriUtil {

    static String toAbsoluteUri(final String srcUri, final String pathOrUri) {
        final String absUri;
        if(0 == pathOrUri.indexOf('/')) { // this is the relative path
            final int srcUriPathStartPos;
            final var srcUriSchemaMarkerPos = srcUri.indexOf(':');
            if(
                srcUriSchemaMarkerPos > 0
                    &&
                srcUriSchemaMarkerPos < srcUri.length() - 2
                    &&
                '/' == srcUri.charAt(srcUriSchemaMarkerPos + 1)
                    &&
                '/' == srcUri.charAt(srcUriSchemaMarkerPos + 2)
            ) {
                srcUriPathStartPos = srcUri.indexOf('/', srcUriSchemaMarkerPos + 3);
            } else {
                srcUriPathStartPos = srcUri.indexOf('/');
            }
            if(srcUriPathStartPos > 0) {
                absUri = srcUri.substring(0, srcUriPathStartPos) + pathOrUri;
            } else {
                absUri = srcUri + pathOrUri;
            }
        } else { // this is the full URI
            absUri = pathOrUri;
        }
        return absUri;
    }
}
