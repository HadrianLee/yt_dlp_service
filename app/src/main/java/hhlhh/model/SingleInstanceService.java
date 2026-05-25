package hhlhh.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SingleInstanceService implements AutoCloseable {

    private static final int SINGLE_INSTANCE_PORT = 47542;
    private static final String SHOW_COMMAND = "hhlhh.show";
    private static final String OK_RESPONSE = "ok";

    private ServerSocket serverSocket;

    public boolean acquire(Runnable showExistingInstanceAction) {
        if (serverSocket != null) {
            return true;
        }

        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), SINGLE_INSTANCE_PORT), 1);
            serverSocket = socket;
            startSignalListener(showExistingInstanceAction);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean signalExistingInstance() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), SINGLE_INSTANCE_PORT), 1000);
            socket.setSoTimeout(1000);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            writer.println(SHOW_COMMAND);
            return OK_RESPONSE.equals(reader.readLine());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (serverSocket == null) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            // The process is exiting, so there is nothing useful to recover here.
        } finally {
            serverSocket = null;
        }
    }

    private void startSignalListener(Runnable showExistingInstanceAction) {
        Thread thread = new Thread(() -> {
            while (serverSocket != null && !serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    handleSignal(socket, showExistingInstanceAction);
                } catch (IOException e) {
                    // Ignore malformed local signals and continue listening.
                }
            }
        }, "single-instance-listener");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleSignal(Socket socket, Runnable showExistingInstanceAction) throws IOException {
        socket.setSoTimeout(1000);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

        if (SHOW_COMMAND.equals(reader.readLine())) {
            writer.println(OK_RESPONSE);
            if (showExistingInstanceAction != null) {
                showExistingInstanceAction.run();
            }
        }
    }
}
