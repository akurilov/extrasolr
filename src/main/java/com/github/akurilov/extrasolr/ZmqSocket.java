package com.github.akurilov.extrasolr;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public final class ZmqSocket {

    private ZContext ctx = null;
    private SocketType socketType = null;
    private String host = null;
    private int port = -1;
    private String topic = null;

    private ZmqSocket() {
    }

    public static ZmqSocket builder() {
        return new ZmqSocket();
    }

    public final ZmqSocket context(final ZContext ctx) {
        this.ctx = ctx;
        return this;
    }

    public final ZmqSocket socketType(final SocketType socketType) {
        this.socketType = socketType;
        return this;
    }

    public final ZmqSocket host(final String host) {
        this.host = host;
        return this;
    }

    public final ZmqSocket port(final int port) {
        this.port = port;
        return this;
    }

    public final ZmqSocket topic(final String topic) {
        this.topic = topic;
        return this;
    }

    public final ZMQ.Socket build() {
        if(null == ctx) {
            throw new IllegalStateException("No context specified");
        }
        if(null == socketType) {
            throw new IllegalStateException("No socket type specified");
        }
        final var socket = ctx.createSocket(socketType);
        if(null == host) {
            throw new IllegalStateException("No host specified");
        }
        if(-1 == port) {
            throw new IllegalStateException("No port specified");
        }
        final var uri = "tcp://" + host + ":" + port;
        if(!socket.connect(uri)) {
            throw new IllegalStateException("Failed to connect to " + uri);
        }
        if(null == topic) {
            throw new IllegalStateException("No topic specified");
        }
        if(!socket.subscribe(topic)) {
            throw new IllegalStateException("Failed to subscribe the source socket to the \"" + topic + "\" topic");
        }
        return socket;
    }
}
