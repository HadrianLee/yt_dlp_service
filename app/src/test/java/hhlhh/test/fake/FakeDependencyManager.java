package hhlhh.test.fake;

import java.nio.file.Files;
import java.nio.file.Path;

import hhlhh.model.DependencyManager;

public class FakeDependencyManager extends DependencyManager {

    private final Path binDirectory;
    private final String ytDlpBinaryName;
    private final String ffmpegBinaryName;

    public FakeDependencyManager(FakeAppPaths appPaths) {
        this(appPaths.binDirectory(), "yt-dlp", "ffmpeg");
    }

    public FakeDependencyManager(Path rootDirectory) {
        this(rootDirectory.resolve("bin"), "yt-dlp", "ffmpeg");
    }

    private FakeDependencyManager(Path binDirectory, String ytDlpBinaryName, String ffmpegBinaryName) {
        this.binDirectory = binDirectory;
        this.ytDlpBinaryName = ytDlpBinaryName;
        this.ffmpegBinaryName = ffmpegBinaryName;
    }

    @Override
    public void ensureDependenciesExist() throws Exception {
        Files.createDirectories(binDirectory);
        writeFakeBinary(binDirectory.resolve(ytDlpBinaryName));
        writeFakeBinary(binDirectory.resolve(ffmpegBinaryName));
    }

    @Override
    public String getBinDirectoryPath() {
        return binDirectory.toString();
    }

    @Override
    public String getYtDlpBinaryName() {
        return ytDlpBinaryName;
    }

    @Override
    public String getFmpegbinaryName() {
        return ffmpegBinaryName;
    }

    public Path ytDlpPath() {
        return binDirectory.resolve(ytDlpBinaryName);
    }

    public Path ffmpegPath() {
        return binDirectory.resolve(ffmpegBinaryName);
    }

    private void writeFakeBinary(Path binaryPath) throws Exception {
        if (!Files.exists(binaryPath)) {
            Files.writeString(binaryPath, "fake binary");
        }
        binaryPath.toFile().setExecutable(true);
    }
}
