package hhlhh.model;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SingleInstanceService implements AutoCloseable {

    private static final String LOCK_FILE_NAME = "single-instance.lock";

    private final Path lockPath;
    private FileChannel lockChannel;
    private FileLock lock;

    public SingleInstanceService() {
        this(AppPaths.appDirectory().resolve(LOCK_FILE_NAME));
    }

    SingleInstanceService(Path lockPath) {
        this.lockPath = lockPath;
    }

    public boolean acquire() {
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
            return true;
        } catch (IOException | OverlappingFileLockException e) {
            close();
            return false;
        }
    }

    @Override
    public void close() {
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
}
