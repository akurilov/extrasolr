package com.github.akurilov.extrasolr;

public interface Config {

    String[] QUEUE_HOSTS = new String[] {
        "localhost:4222",
    };
    String SUBJECT_FETCH = "fetch";
    String SUBJECT_PARSE = "parse";
    String SUBJECT_INDEX = "index";
}
