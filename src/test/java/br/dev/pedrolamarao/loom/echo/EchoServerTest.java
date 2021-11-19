package br.dev.pedrolamarao.loom.echo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoServerTest
{
    static final int messageSize = 3072;

    static final int clients = 11000;

    @Test
    void platformThreads () throws Exception
    {
        try (var server = EchoServer.create( Thread.ofPlatform().factory() )) { test(server); }
    }

    @Test
    void virtualThreads () throws Exception
    {
        try (var server = EchoServer.create( Thread.ofVirtual().factory() )) { test(server); }
    }

    void test (EchoServer server) throws Exception
    {
        final var port = ThreadLocalRandom.current().nextInt(10000, 20000);
        final var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        server.bind(address);
        server.start();

        final var message = new byte[messageSize];
        ThreadLocalRandom.current().nextBytes(message);

        final var connects = new HashMap<Future<?>, AsynchronousSocketChannel>();

        try
        {
            for (int i = 0; i != clients; ++i) {
                final var socket = AsynchronousSocketChannel.open();
                connects.put( socket.connect(address), socket );
            }

            final var sends = new HashMap<Future<?>, AsynchronousSocketChannel>();

            for (var connect : connects.keySet()) {
                connect.get();
                final var socket = connects.get(connect);
                sends.put( socket.write( ByteBuffer.wrap(message) ), socket );
            }

            final var receives = new HashMap<Future<?>, ByteBuffer>();

            for (var send : sends.keySet()) {
                final var sent = send.get();
                assertEquals(messageSize, sent);
                final var socket = sends.get(send);
                final var buffer = ByteBuffer.allocate(messageSize);
                receives.put( socket.read(buffer), buffer );
            }

            for (var receive : receives.keySet()) {
                final var received = receive.get();
                assertEquals(messageSize, received);
                final var buffer = receives.get(receive);
                assertArrayEquals(message, buffer.array());
            }
        }
        finally
        {
            connects.values().forEach( socket -> {
                try { socket.close(); } catch (IOException ignored) {}
            } );
        }
    }
}