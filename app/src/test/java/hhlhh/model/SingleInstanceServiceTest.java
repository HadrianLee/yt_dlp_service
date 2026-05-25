package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void acquireBlocksSecondInstanceUntilClosed() throws Exception {
        int port = findAvailablePort();
        Path lockPath = tempDir.resolve("single-instance.lock");
        SingleInstanceService first = new SingleInstanceService(port, lockPath);
        SingleInstanceService second = new SingleInstanceService(port, lockPath);

        try {
            assertTrue(first.acquire(null));
            assertFalse(second.acquire(null));

            first.close();

            assertTrue(second.acquire(null));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void signalExistingInstanceRunsShowAction() throws Exception {
        int port = findAvailablePort();
        Path lockPath = tempDir.resolve("single-instance.lock");
        SingleInstanceService first = new SingleInstanceService(port, lockPath);
        CountDownLatch signalReceived = new CountDownLatch(1);

        try {
            assertTrue(first.acquire(signalReceived::countDown));

            SingleInstanceService second = new SingleInstanceService(port, lockPath);
            try {
                assertTrue(second.signalExistingInstance());
            } finally {
                second.close();
            }

            assertTrue(signalReceived.await(2, TimeUnit.SECONDS));
        } finally {
            first.close();
        }
    }

    @Test
    void acquireStillSucceedsWhenSignalPortIsUnavailable() throws Exception {
        int port = findAvailablePort();
        Path lockPath = tempDir.resolve("single-instance.lock");

        try (ServerSocket unavailablePort = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            SingleInstanceService service = new SingleInstanceService(port, lockPath);
            try {
                assertTrue(service.acquire(null));
            } finally {
                service.close();
            }
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
