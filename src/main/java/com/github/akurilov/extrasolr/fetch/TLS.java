package com.github.akurilov.extrasolr.fetch;

public interface TLS {

    String[] TLS_PROTOCOLS = {
        "SSLv3",
        "TLSv1",
        "TLSv1.1",
        "TLSv1.2",
    };
    String[] TLS_CIPHER_SUTES_EXCLUDE = {
    };

    static boolean isCipherSuiteAllowed(final String cipherSuite) {
        for(final String cs : TLS_CIPHER_SUTES_EXCLUDE) {
            if(cipherSuite.equals(cs)) {
                return false;
            }
        }
        return true;
    }
}
