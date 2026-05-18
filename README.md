# yt-dlp Service

A small personal-use JavaFX application for downloading media with `yt-dlp`.

## Notice

This project is made for personal use. It is provided as-is, with no warranty,
support, or guarantee that it will work correctly on your machine.

Use it at your own risk. You are responsible for how you use this software and
for complying with any applicable laws, platform terms, copyright rules, and
network policies.

## For Users

Compiled ZIP builds are available from the project releases. Download and
extract the ZIP, then run `YtDlpService.exe` directly.

You do not need to build the project or package it with `jpackage`.

`yt-dlp` and `ffmpeg` are installed automatically the first time the app runs.
They are stored in:

```text
%USERPROFILE%\.yt_dlp_service\
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
