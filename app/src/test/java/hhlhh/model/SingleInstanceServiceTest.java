package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void acquireBlocksSecondInstanceUntilClosed() {
        Path lockPath = tempDir.resolve("single-instance.lock");
        SingleInstanceService first = new SingleInstanceService(lockPath);
        SingleInstanceService second = new SingleInstanceService(lockPath);

        try {
            assertTrue(first.acquire());
            assertFalse(second.acquire());

            first.close();

            assertTrue(second.acquire());
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void acquireIsIdempotentForSameService() {
        Path lockPath = tempDir.resolve("single-instance.lock");
        SingleInstanceService service = new SingleInstanceService(lockPath);

        try {
            assertTrue(service.acquire());
            assertTrue(service.acquire());
        } finally {
            service.close();
        }
    }

    @Test
    void closeWithoutAcquireIsAllowed() {
        SingleInstanceService service = new SingleInstanceService(tempDir.resolve("single-instance.lock"));

        service.close();

        assertTrue(service.acquire());
        service.close();
    }
}
