package hhlhh.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SingleInstanceService implements AutoCloseable {

    private static final int DEFAULT_SINGLE_INSTANCE_PORT = 47542;
    private static final String LOCK_FILE_NAME = "single-instance.lock";
    private static final String SHOW_COMMAND = "hhlhh.show";
    private static final String OK_RESPONSE = "ok";

    private final int singleInstancePort;
    private final Path lockPath;
    private FileChannel lockChannel;
    private FileLock lock;
    private ServerSocket serverSocket;

    public SingleInstanceService() {
        this(DEFAULT_SINGLE_INSTANCE_PORT, AppPaths.appDirectory().resolve(LOCK_FILE_NAME));
    }

    SingleInstanceService(int singleInstancePort) {
        this(singleInstancePort, AppPaths.appDirectory().resolve(LOCK_FILE_NAME));
    }

    SingleInstanceService(int singleInstancePort, Path lockPath) {
        this.singleInstancePort = singleInstancePort;
        this.lockPath = lockPath;
    }

    public boolean acquire(Runnable showExistingInstanceAction) {
        if (lock != null && lock.isValid()) {
            return true;
        }

        try {
            Files.createDirectories(lockPath.getParent());
            lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = lockChannel.tryLock();
            if (lock == null) {
                close();
                return false;
            }
            startSignalListener(showExistingInstanceAction);
            return true;
        } catch (IOException | OverlappingFileLockException e) {
            close();
            return false;
        }
    }

    public boolean signalExistingInstance() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), singleInstancePort), 1000);
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
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // The process is exiting, so there is nothing useful to recover here.
        } finally {
            serverSocket = null;
        }

        try {
            if (lock != null) {
                lock.release();
            }
        } catch (IOException e) {
            // The process is exiting, so there is nothing useful to recover here.
        } finally {
            lock = null;
        }

        try {
            if (lockChannel != null) {
                lockChannel.close();
            }
        } catch (IOException e) {
            // The process is exiting, so there is nothing useful to recover here.
        } finally {
            lockChannel = null;
        }
    }

    private void startSignalListener(Runnable showExistingInstanceAction) {
        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), singleInstancePort), 1);
            serverSocket = socket;
        } catch (IOException e) {
            return;
        }

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
