package ru.techmail.maxim.server.blockingServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.techmail.maxim.server.filesContainer.FileServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.IllegalBlockingModeException;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private int port;
    private int coresCount;

    public Main() {
        port = 8080;
        coresCount = 2;
    }

    private void parseArgs(String[] args, int iter) {
        switch (args[iter]) {
            case "-p" :
                try {
                    port = Integer.valueOf(args[iter + 1]);
                } catch (NumberFormatException e) {
                    LOG.error("Wrong format of port value (used default port: 8080)");
                }
                break;
            case "-r" :
                FileServer.setFilesDir(args[iter + 1]);
                break;
            case "-c" :
                try {
                    port = Integer.valueOf(args[iter + 1]);
                } catch (NumberFormatException e) {
                    LOG.error("Wrong format of port value (used default cores count: 2)");
                }
                break;
        }
    }

    public void readArgs(String[] args) {
        switch (args.length) {
            case 2 :
                parseArgs(args, 0);
                break;
            case 4 :
                parseArgs(args, 0);
                parseArgs(args, 2);
                break;
            case 6 :
                parseArgs(args, 0);
                parseArgs(args, 2);
                parseArgs(args, 4);
                break;
        }
    }

    public int getPort() {
        return port;
    }

    public int getCoresCount() {
        return coresCount;
    }

	public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.readArgs(args);
		try(ServerSocket serverSocket = new ServerSocket(main.port)) {
            AtomicInteger threadsCount = new AtomicInteger(0);
			while(true) {
				try {
                    while (threadsCount.get() > main.coresCount * 10) {
                        Thread.sleep(10);
                    }
					Socket clientSocket = serverSocket.accept();
                    LOG.debug("Connetion accepted, starting new thread");
                    // добавить счётчик активных тредов (чтобы проверять на max количество)
                    threadsCount.incrementAndGet();
                    new Thread(new Worker(clientSocket, threadsCount)).start();
				} catch (IllegalBlockingModeException e) {
                    LOG.error("No connection ready to be accepted");
                } catch (IOException e) {
				    LOG.error("Accept failed");
				}
			}
		} catch (IOException e) {
		    LOG.error("Failed starting server on port: " + main.port);
		    System.exit(-1);
		}
	}
}
