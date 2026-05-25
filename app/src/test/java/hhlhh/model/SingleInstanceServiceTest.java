package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class SingleInstanceServiceTest {

    @Test
    void acquireBlocksSecondInstanceUntilClosed() throws Exception {
        int port = findAvailablePort();
        SingleInstanceService first = new SingleInstanceService(port);
        SingleInstanceService second = new SingleInstanceService(port);

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
        SingleInstanceService first = new SingleInstanceService(port);
        CountDownLatch signalReceived = new CountDownLatch(1);

        try {
            assertTrue(first.acquire(signalReceived::countDown));

            SingleInstanceService second = new SingleInstanceService(port);
            assertTrue(second.signalExistingInstance());

            assertTrue(signalReceived.await(2, TimeUnit.SECONDS));
        } finally {
            first.close();
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
