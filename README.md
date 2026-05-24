# yt-dlp Service

A small personal-use JavaFX application for downloading media with `yt-dlp`.

## Notice

This project is made for personal use. It is provided as-is, with no warranty,
support, or guarantee that it will work correctly on your machine.

Use it at your own risk. You are responsible for how you use this software and
for complying with any applicable laws, platform terms, copyright rules, and
network policies.

# INSTALLATION

<!-- MANPAGE: BEGIN EXCLUDED SECTION -->
[![Windows](https://img.shields.io/badge/-Windows_x64-blue.svg?style=for-the-badge&logo=windows)](https://github.com/HadrianLee/yt_dlp_service/releases/latest/download/YtDlpService.zip)
[![Linux](https://img.shields.io/badge/-Linux_x64-red.svg?style=for-the-badge&logo=linux)](https://github.com/HadrianLee/yt_dlp_service/releases/latest/download/YtDlpService-linux.zip)
[![MacOS](https://img.shields.io/badge/-MacOS-lightblue.svg?style=for-the-badge&logo=apple)](https://github.com/HadrianLee/yt_dlp_service/releases/latest/download/YtDlpService-macos.zip)
[![Source Zip](https://img.shields.io/badge/-Source_zip-green.svg?style=for-the-badge)](https://github.com/HadrianLee/yt_dlp_service/archive/refs/heads/main.zip)
[![All versions](https://img.shields.io/badge/-All_Versions-lightgrey.svg?style=for-the-badge)](https://github.com/HadrianLee/yt_dlp_service/releases)
<!-- MANPAGE: END EXCLUDED SECTION -->

Compiled ZIP builds are available from the project releases. Download the ZIP
for your operating system and extract it. On Windows, run `YtDlpService.exe`.
On macOS, open `YtDlpService.app`. On Linux, run the `YtDlpService` launcher.

You do not need to build the project or package it with `jpackage`.

`yt-dlp` and `ffmpeg` are installed automatically the first time the app runs.
App settings, consent state, `yt-dlp`, and `ffmpeg` are stored in:

```text
Windows: %USERPROFILE%\.yt_dlp_service\
macOS:   ~/.yt_dlp_service/
Linux:   ~/.yt_dlp_service/
```

On first run, the app may take a little longer while it downloads and sets up
`yt-dlp` and `ffmpeg`.

## For Development

### Project Layout

- `app/` - Maven Java application source
- `app/src/main/java/` - JavaFX application code

### Requirements

- Java
- Maven

### Running From Source

From the `app` directory:

```powershell
mvn javafx:run
```

### Unit Tests

From the `app` directory:

```powershell
mvn test
```

The test suite lives in:

```text
app/src/test/java/
```
