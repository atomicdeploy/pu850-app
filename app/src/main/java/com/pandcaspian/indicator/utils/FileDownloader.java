package com.pandcaspian.indicator.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modern file downloader using ExecutorService.
 * <p>
 * Features:
 * - Resume/pause support with HTTP Range headers
 * - Metadata fetching before download
 * - Progress tracking with callbacks
 * - Proper error handling
 * - Thread-safe state management
 * - WakeLock management for background downloads
 */
public class FileDownloader {

    private static final String TAG = "FileDownloader";
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Download configuration
    private String fileUrl;
    private File targetFile;
    private PowerManager.WakeLock wakeLock;

    // State management
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicLong downloadedBytes = new AtomicLong(0);
    private long totalBytes = -1;
    private Future<?> downloadFuture;

    // Metadata
    private FileMetadata metadata;

    // Callbacks
    private OnMetadataListener onMetadata;
    private OnProgressListener onProgress;
    private OnCompletedListener onCompleted;
    private OnErrorListener onError;

    /**
     * File metadata
     */
    public static class FileMetadata {
        public final long fileSize;
        public final String fileName;
        public final String lastModified;
        public final boolean supportsResume;
        public final String contentType;

        public FileMetadata(long fileSize, String fileName, String lastModified,
                           boolean supportsResume, String contentType) {
            this.fileSize = fileSize;
            this.fileName = fileName;
            this.lastModified = lastModified;
            this.supportsResume = supportsResume;
            this.contentType = contentType;
        }
    }

    /**
     * Download state
     */
    public enum State {
        IDLE,
        FETCHING_METADATA,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        ERROR,
        CANCELLED
    }

    private volatile State state = State.IDLE;

    public FileDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    // ========== Configuration ==========

    public FileDownloader url(@NonNull String url) {
        this.fileUrl = url;
        return this;
    }

    public FileDownloader targetFile(@NonNull File file) {
        this.targetFile = file;
        return this;
    }

    public FileDownloader onMetadata(OnMetadataListener listener) {
        this.onMetadata = listener;
        return this;
    }

    public FileDownloader onProgress(OnProgressListener listener) {
        this.onProgress = listener;
        return this;
    }

    public FileDownloader onCompleted(OnCompletedListener listener) {
        this.onCompleted = listener;
        return this;
    }

    public FileDownloader onError(OnErrorListener listener) {
        this.onError = listener;
        return this;
    }

    // ========== Control Methods ==========

    /**
     * Fetch file metadata without downloading
     */
    public void fetchMetadata() {
        if (fileUrl == null) {
            notifyError(new IllegalStateException("URL not set"));
            return;
        }

        state = State.FETCHING_METADATA;
        executor.execute(this::doFetchMetadata);
    }

    /**
     * Start or resume download
     */
    public void start() {
        if (fileUrl == null || targetFile == null) {
            notifyError(new IllegalStateException("URL or target file not set"));
            return;
        }

        if (state == State.DOWNLOADING) {
            Log.w(TAG, "Download already in progress");
            return;
        }

        paused.set(false);
        cancelled.set(false);
        state = State.DOWNLOADING;

        acquireWakeLock();
        downloadFuture = executor.submit(this::doDownload);
    }

    /**
     * Pause the download (can be resumed)
     */
    public void pause() {
        if (state != State.DOWNLOADING) return;
        paused.set(true);
        state = State.PAUSED;
        releaseWakeLock();
    }

    /**
     * Resume a paused download
     */
    public void resume() {
        if (state != State.PAUSED) return;
        start();
    }

    /**
     * Cancel the download
     */
    public void cancel() {
        cancelled.set(true);
        state = State.CANCELLED;
        if (downloadFuture != null) {
            downloadFuture.cancel(true);
        }
        releaseWakeLock();
    }

    /**
     * Check if download is cancelled
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Get current state
     */
    public State getState() {
        return state;
    }

    /**
     * Get downloaded bytes
     */
    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    /**
     * Get total bytes (-1 if unknown)
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Get progress percentage (0-100)
     */
    public int getProgressPercent() {
        if (totalBytes <= 0) return 0;
        return (int) ((downloadedBytes.get() * 100) / totalBytes);
    }

    /**
     * Get file metadata
     */
    public FileMetadata getMetadata() {
        return metadata;
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        cancel();
        executor.shutdown();
    }

    // ========== Private Implementation ==========

    private void doFetchMetadata() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                notifyError(new IOException("Server returned HTTP " + responseCode));
                return;
            }

            metadata = parseMetadata(connection, url);
            totalBytes = metadata.fileSize;

            mainHandler.post(() -> {
                if (onMetadata != null) {
                    onMetadata.onMetadata(metadata);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error fetching metadata", e);
            notifyError(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void doDownload() {
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;

        try {
            // Check for existing partial download
            long existingBytes = 0;
            if (targetFile.exists()) {
                existingBytes = targetFile.length();
            }

            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");

            // Request resume if partial download exists
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=" + existingBytes + "-");
            }

            int responseCode = connection.getResponseCode();

            // Handle response codes
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Server doesn't support resume or sent full file
                existingBytes = 0;
                downloadedBytes.set(0);
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                // Resume supported
                downloadedBytes.set(existingBytes);
            } else {
                throw new IOException("Server returned HTTP " + responseCode + ": " + connection.getResponseMessage());
            }

            // Parse metadata if needed
            if (metadata == null) {
                metadata = parseMetadata(connection, url);
            }

            // Get total file size
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                totalBytes = existingBytes + contentLength;
            }

            // Notify metadata
            if (onMetadata != null) {
                final FileMetadata meta = metadata;
                mainHandler.post(() -> onMetadata.onMetadata(meta));
            }

            // Open streams
            input = new BufferedInputStream(connection.getInputStream(), BUFFER_SIZE);

            // Open file (append if resuming)
            boolean append = existingBytes > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL;
            output = new FileOutputStream(targetFile, append);

            // Download loop
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long lastProgressUpdate = 0;

            while ((bytesRead = input.read(buffer)) != -1) {
                if (cancelled.get()) {
                    state = State.CANCELLED;
                    return;
                }

                if (paused.get()) {
                    state = State.PAUSED;
                    return;
                }

                output.write(buffer, 0, bytesRead);
                downloadedBytes.addAndGet(bytesRead);

                // Throttle progress updates
                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate >= 100) {
                    lastProgressUpdate = now;
                    notifyProgress();
                }
            }

            output.flush();
            state = State.COMPLETED;
            notifyCompleted();

        } catch (Exception e) {
            Log.e(TAG, "Download error", e);
            state = State.ERROR;
            notifyError(e);
        } finally {
            closeQuietly(input);
            closeQuietly(output);
            if (connection != null) {
                connection.disconnect();
            }
            releaseWakeLock();
        }
    }

    private FileMetadata parseMetadata(HttpURLConnection connection, URL url) {
        long fileSize = connection.getContentLengthLong();

        // Get filename
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        String fileName = URLUtil.guessFileName(url.toString(), contentDisposition, null);

        if (contentDisposition != null && !contentDisposition.isEmpty()) {
            String extracted = contentDisposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
            if (!extracted.isEmpty() && !extracted.equals(contentDisposition)) {
                fileName = extracted;
            }
        }

        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.ISO_8859_1.name());
        } catch (Exception e) {
            Log.w(TAG, "Error decoding filename", e);
        }

        String lastModified = connection.getHeaderField("Last-Modified");
        String acceptRanges = connection.getHeaderField("Accept-Ranges");
        boolean supportsResume = "bytes".equalsIgnoreCase(acceptRanges);
        String contentType = connection.getContentType();

        return new FileMetadata(fileSize, fileName, lastModified, supportsResume, contentType);
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":download");
                wakeLock.setReferenceCounted(false);
            }
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(30 * 60 * 1000L); // 30 minute timeout
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void notifyProgress() {
        if (onProgress != null) {
            final long downloaded = downloadedBytes.get();
            final long total = totalBytes;
            final int percent = getProgressPercent();
            mainHandler.post(() -> onProgress.onProgress(downloaded, total, percent));
        }
    }

    private void notifyCompleted() {
        if (onCompleted != null) {
            mainHandler.post(() -> onCompleted.onCompleted(targetFile));
        }
    }

    private void notifyError(Exception e) {
        state = State.ERROR;
        if (onError != null) {
            mainHandler.post(() -> onError.onError(e));
        }
    }

    // ========== Callback Interfaces ==========

    public interface OnMetadataListener {
        void onMetadata(FileMetadata metadata);
    }

    public interface OnProgressListener {
        void onProgress(long downloadedBytes, long totalBytes, int percent);
    }

    public interface OnCompletedListener {
        void onCompleted(File file);
    }

    public interface OnErrorListener {
        void onError(Exception e);
    }
}
