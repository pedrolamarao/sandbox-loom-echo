package br.dev.pedrolamarao.loom.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.*;

public class EchoServer implements AutoCloseable
{
    final ArrayList<InetSocketAddress> addresses = new ArrayList<>();

    final ExecutorService executor;

    final Logger logger = LoggerFactory.getLogger(EchoServer.class);

    final ConcurrentHashMap<InetSocketAddress, ServerSocketChannel> servers = new ConcurrentHashMap<>();

    EchoServer (ExecutorService executor)
    {
        this.executor = executor;
    }

    public static EchoServer create (ThreadFactory threadFactory)
    {
        return new EchoServer(Executors.newThreadPerTaskExecutor(threadFactory));
    }

    public void close ()
    {
        executor.shutdownNow();
    }

    public void bind (InetSocketAddress address)
    {
        addresses.add(address);
    }

    public void start ()
    {
        addresses.forEach( address -> executor.submit( accept(address) ) );
    }

    public void stop ()
    {
        servers.forEach( (address, server) -> {
            try { server.close(); }
            catch (IOException e) { logger.debug("stop: failure", e); }
        });
        servers.clear();
    }

    //

    Callable<Void> accept (InetSocketAddress address)
    {
        return () ->
        {
            final var server = ServerSocketChannel.open();
            server.bind(address);
            servers.put(address, server);
            while (true) {
                final var client = server.accept();
                logger.info("accept: {}", client.getRemoteAddress());
                executor.submit( work(client) );
            }
        };
    }

    Callable<Void> work (SocketChannel client)
    {
        return () ->
        {
            final var localAddress = client.getLocalAddress();
            final var buffer = ByteBuffer.allocate(4096);
            while (true) {
                buffer.clear();
                final var read = client.read(buffer);
                if (read == -1) return null;
                logger.info("read: {}: {}", localAddress, read);
                buffer.flip();
                final var written = client.write(buffer);
                logger.info("write: {}, {}", localAddress, written);
            }
        };
    }
}