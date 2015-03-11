package ru.techmail.maxim.server.nonBlockingServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {
    Selector clientSelector;

    private void start(int port, int threads) throws IOException {
        clientSelector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        serverChannel.bind(socketAddress);
        serverChannel.register(clientSelector, SelectionKey.OP_ACCEPT);
        Executor executor = Executors.newFixedThreadPool(threads);
        while (true) {
            try {
                while (clientSelector.select(50) == 0);
                Set<SelectionKey> readySet = clientSelector.selectedKeys();
                for (Iterator<SelectionKey> iter = readySet.iterator(); iter.hasNext();) {
                    final SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) {
                        acceptClient(serverChannel);
                    } else {
                        key.interestOps(0);
                        executor.execute(() -> {
                            try {
                                handleClient(key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptClient(ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientSocket = serverChannel.accept();
        clientSocket.configureBlocking(false);
        SelectionKey key = clientSocket.register(clientSelector, SelectionKey.OP_READ);
        Worker worker = new Worker(clientSocket);
        key.attach(worker);
    }

    private void handleClient(SelectionKey key) throws IOException {
        Worker worker = (Worker) key.attachment();
        if (key.isReadable()) {
            worker.read(key);
        } else {
            worker.write(key);
        }
        clientSelector.wakeup();
    }

    public static void main(String[] args) throws IOException {
        ru.techmail.maxim.server.blockingServer.Main main = new ru.techmail.maxim.server.blockingServer.Main();
        main.readArgs(args);
        new Main().start(main.getPort(), main.getCoresCount() * 2);
    }
}
