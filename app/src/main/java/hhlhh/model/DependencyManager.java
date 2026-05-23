package hhlhh.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DependencyManager {

    // Public APIs for fetching upstream builds
    private static final String YTDLP_WINDOWS_URL =
            "https://github.com/yt-dlp/yt-dlp/releases/download/2026.03.17/yt-dlp.exe";
    private static final String YTDLP_LINUX_URL =
            "https://github.com/yt-dlp/yt-dlp/releases/download/2026.03.17/yt-dlp";
    private static final String FFMPEG_WINDOWS_64_URL =
            "https://github.com/ffbinaries/ffbinaries-prebuilt/releases/download/v6.1/ffmpeg-6.1-win-64.zip";
    private static final String FFMPEG_LINUX_64_URL =
            "https://github.com/ffbinaries/ffbinaries-prebuilt/releases/download/v6.1/ffmpeg-6.1-linux-64.zip";

    private final HttpClient httpClient;

    public DependencyManager() {
        // Configure standard HTTP Client following redirects automatically
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

        private enum OS { WINDOWS, MAC_OS, LINUX, UNSUPPORTED }

    private OS detectOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) return OS.WINDOWS;
        if (osName.contains("mac")) return OS.MAC_OS;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) return OS.LINUX;
        return OS.UNSUPPORTED;
    }

    public String getYtDlpBinaryName() {
        return detectOS() == OS.WINDOWS ? "yt-dlp.exe" : "yt-dlp";
    }

    public String getFmpegbinaryName() {
        return detectOS() == OS.WINDOWS ? "ffmpeg.exe" : "ffmpeg";
    }

    private String resolveYtDlpDownloadUrl(OS os) {
        return switch (os) {
            case WINDOWS -> YTDLP_WINDOWS_URL;
            case LINUX -> YTDLP_LINUX_URL;
            case MAC_OS -> throw new IllegalStateException("No yt-dlp macOS download URL configured yet.");
            default -> throw new IllegalStateException("Unsupported Operating System Platform.");
        };
    }

    private String resolveFfmpegDownloadUrl(OS os) {
        return switch (os) {
            case WINDOWS -> FFMPEG_WINDOWS_64_URL;
            case LINUX -> FFMPEG_LINUX_64_URL;
            case MAC_OS -> throw new IllegalStateException("No FFmpeg macOS download URL configured yet.");
            default -> throw new IllegalStateException("Unsupported Operating System Platform.");
        };
    }


        private void downloadFile(String fileUrl, Path destinationPath) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fileUrl)).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Asset download failed. HTTP Code: " + response.statusCode());
        }

        Files.createDirectories(destinationPath.getParent());
        try (InputStream is = response.body();
             FileOutputStream fos = new FileOutputStream(destinationPath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void downloadAndExtractZip(String zipUrl, Path targetExeName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(zipUrl)).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("FFmpeg ZIP download failed from " + zipUrl + ". HTTP Code: " + response.statusCode());
        }

        // Process ZIP archive structures over incoming stream
        try (ZipInputStream zis = new ZipInputStream(response.body())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Focus exclusively on extracted executable names (e.g. ffmpeg / ffmpeg.exe)
                if (entry.getName().equalsIgnoreCase(targetExeName.getFileName().toString()) || 
                    entry.getName().endsWith("/" + targetExeName.getFileName().toString())) {
                    
                    Files.createDirectories(targetExeName.getParent());
                    try (FileOutputStream fos = new FileOutputStream(targetExeName.toFile())) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                    zis.closeEntry();
                    return;
                }
                zis.closeEntry();
            }
        }
    }


    public void ensureDependenciesExist() throws Exception {
        OS currentOS = detectOS();
        if (currentOS == OS.UNSUPPORTED) {
            throw new IllegalStateException("Unsupported Operating System Platform.");
        }

        File binDir = AppPaths.binDirectory().toFile();
        if (!binDir.exists()) {
            binDir.mkdirs();
        }

        Path ytDlpFile = AppPaths.binDirectory().resolve(getYtDlpBinaryName());
        Path ffmpegFile = AppPaths.binDirectory().resolve(getFmpegbinaryName());

        // --- Handle Task 1: yt-dlp check and update ---
        if (!ytDlpFile.toFile().exists()) {
            System.out.println("Downloading latest yt-dlp build...");
            String downloadUrl = resolveYtDlpDownloadUrl(currentOS);
            downloadFile(downloadUrl, ytDlpFile);
            
            // Critical for Unix environments: grant Execution permission access flags
            ytDlpFile.toFile().setExecutable(true);
        }

        // --- Handle Task 2: FFmpeg check and update ---
        if (!ffmpegFile.toFile().exists()) {
            System.out.println("Downloading latest FFmpeg bundle...");
            String downloadUrl = resolveFfmpegDownloadUrl(currentOS);
            downloadAndExtractZip(downloadUrl, ffmpegFile);
            
            ffmpegFile.toFile().setExecutable(true);
        }
        System.out.println("All external dependency environments compiled and verified.");
    }

    public void repairDependencies() throws Exception {
        Path binDirectory = Path.of(getBinDirectoryPath());
        Files.createDirectories(binDirectory);
        Files.deleteIfExists(binDirectory.resolve(getYtDlpBinaryName()));
        Files.deleteIfExists(binDirectory.resolve(getFmpegbinaryName()));
        ensureDependenciesExist();
    }

    public String getBinDirectoryPath() {
        return AppPaths.binDirectory().toAbsolutePath().toString();
    }
}
