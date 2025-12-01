package com.pandcaspian.indicator.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modern file downloader using ExecutorService instead of deprecated AsyncTask.
 * Features:
 * - Resume/pause support with Range header
 * - Metadata checking before download
 * - Proper error handling with callbacks
 * - Thread-safe state management
 * - WakeLock management for background downloads
 */
public class ModernFileDownloader {

    private static final String TAG = "ModernFileDownloader";
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;

    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executor;

    private String fileUrl;
    private File targetFile;
    private PowerManager.WakeLock wakeLock;

    // State management
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicLong downloadedBytes = new AtomicLong(0);
    private long totalBytes = -1;
    private Future<?> downloadFuture;

    // File metadata
    private FileMetadata metadata;

    // Callbacks
    private OnMetadataReceivedListener metadataListener;
    private OnProgressListener progressListener;
    private OnCompletedListener completedListener;
    private OnErrorListener errorListener;

    /**
     * File metadata retrieved before download starts
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
     * Download state information
     */
    public enum DownloadState {
        IDLE,
        FETCHING_METADATA,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        ERROR,
        CANCELLED
    }

    private volatile DownloadState state = DownloadState.IDLE;

    public ModernFileDownloader(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Set the file URL to download
     */
    public ModernFileDownloader setUrl(String url) {
        this.fileUrl = url;
        return this;
    }

    /**
     * Set the target file to save the download
     */
    public ModernFileDownloader setTargetFile(File file) {
        this.targetFile = file;
        return this;
    }

    /**
     * Set callback for when file metadata is received
     */
    public ModernFileDownloader setOnMetadataReceived(OnMetadataReceivedListener listener) {
        this.metadataListener = listener;
        return this;
    }

    /**
     * Set callback for progress updates
     */
    public ModernFileDownloader setOnProgress(OnProgressListener listener) {
        this.progressListener = listener;
        return this;
    }

    /**
     * Set callback for when download completes
     */
    public ModernFileDownloader setOnCompleted(OnCompletedListener listener) {
        this.completedListener = listener;
        return this;
    }

    /**
     * Set callback for errors
     */
    public ModernFileDownloader setOnError(OnErrorListener listener) {
        this.errorListener = listener;
        return this;
    }

    /**
     * Fetch file metadata without downloading
     */
    public void fetchMetadata() {
        if (fileUrl == null) {
            notifyError(new IllegalStateException("URL not set"));
            return;
        }

        state = DownloadState.FETCHING_METADATA;
        executor.execute(this::doFetchMetadata);
    }

    /**
     * Start or resume the download
     */
    public void start() {
        if (fileUrl == null || targetFile == null) {
            notifyError(new IllegalStateException("URL or target file not set"));
            return;
        }

        if (state == DownloadState.DOWNLOADING) {
            Log.w(TAG, "Download already in progress");
            return;
        }

        isPaused.set(false);
        isCancelled.set(false);
        state = DownloadState.DOWNLOADING;

        acquireWakeLock();
        downloadFuture = executor.submit(this::doDownload);
    }

    /**
     * Pause the download (can be resumed)
     */
    public void pause() {
        if (state != DownloadState.DOWNLOADING) {
            return;
        }

        isPaused.set(true);
        state = DownloadState.PAUSED;
        releaseWakeLock();
    }

    /**
     * Resume a paused download
     */
    public void resume() {
        if (state != DownloadState.PAUSED) {
            return;
        }

        start();
    }

    /**
     * Cancel the download
     */
    public void cancel() {
        isCancelled.set(true);
        state = DownloadState.CANCELLED;

        if (downloadFuture != null) {
            downloadFuture.cancel(true);
        }

        releaseWakeLock();
    }

    /**
     * Get current download state
     */
    public DownloadState getState() {
        return state;
    }

    /**
     * Get downloaded bytes count
     */
    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    /**
     * Get total bytes to download (-1 if unknown)
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Get download progress as percentage (0-100)
     */
    public int getProgressPercent() {
        if (totalBytes <= 0) return 0;
        return (int) ((downloadedBytes.get() * 100) / totalBytes);
    }

    /**
     * Get the file metadata (available after fetchMetadata or during download)
     */
    public FileMetadata getMetadata() {
        return metadata;
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        cancel();
        executor.shutdown();
    }

    // ========== Private implementation methods ==========

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
                if (metadataListener != null) {
                    metadataListener.onMetadataReceived(metadata);
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
            // Check if we can resume from existing partial download
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

            // Request resume if we have partial download
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
                // Resume supported, continue from where we left off
                downloadedBytes.set(existingBytes);
            } else {
                throw new IOException("Server returned HTTP " + responseCode + ": " + connection.getResponseMessage());
            }

            // Parse metadata if not already fetched
            if (metadata == null) {
                metadata = parseMetadata(connection, url);
            }

            // Get total file size
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                totalBytes = existingBytes + contentLength;
            }

            notifyMetadataReceived();

            // Open streams
            input = new BufferedInputStream(connection.getInputStream(), BUFFER_SIZE);
            
            // Open file for writing (append mode if resuming)
            if (existingBytes > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL) {
                output = new FileOutputStream(targetFile, true);
            } else {
                output = new FileOutputStream(targetFile, false);
            }

            // Download loop
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long lastProgressUpdate = 0;

            while ((bytesRead = input.read(buffer)) != -1) {
                // Check for cancellation
                if (isCancelled.get()) {
                    state = DownloadState.CANCELLED;
                    return;
                }

                // Check for pause
                if (isPaused.get()) {
                    state = DownloadState.PAUSED;
                    return;
                }

                // Write to file
                output.write(buffer, 0, bytesRead);
                downloadedBytes.addAndGet(bytesRead);

                // Throttle progress updates to avoid UI flooding
                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate >= 100) { // Update every 100ms max
                    lastProgressUpdate = now;
                    notifyProgress();
                }
            }

            output.flush();
            state = DownloadState.COMPLETED;
            notifyCompleted();

        } catch (Exception e) {
            Log.e(TAG, "Download error", e);
            state = DownloadState.ERROR;
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
        // Get file size
        long fileSize = connection.getContentLengthLong();

        // Get filename from Content-Disposition or URL
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        String fileName = URLUtil.guessFileName(url.toString(), contentDisposition, null);

        if (contentDisposition != null && !contentDisposition.isEmpty()) {
            String extracted = contentDisposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
            if (!extracted.isEmpty() && !extracted.equals(contentDisposition)) {
                fileName = extracted;
            }
        }

        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            Log.w(TAG, "Error decoding filename", e);
        }

        // Get last modified date
        String lastModified = connection.getHeaderField("Last-Modified");

        // Check if server supports resume
        String acceptRanges = connection.getHeaderField("Accept-Ranges");
        boolean supportsResume = "bytes".equalsIgnoreCase(acceptRanges);

        // Get content type
        String contentType = connection.getContentType();

        return new FileMetadata(fileSize, fileName, lastModified, supportsResume, contentType);
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":download");
            wakeLock.setReferenceCounted(false);
        }
        if (!wakeLock.isHeld()) {
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
            } catch (IOException ignored) {}
        }
    }

    private void notifyMetadataReceived() {
        if (metadataListener != null && metadata != null) {
            mainHandler.post(() -> metadataListener.onMetadataReceived(metadata));
        }
    }

    private void notifyProgress() {
        if (progressListener != null) {
            final long downloaded = downloadedBytes.get();
            final long total = totalBytes;
            final int percent = getProgressPercent();
            mainHandler.post(() -> progressListener.onProgress(downloaded, total, percent));
        }
    }

    private void notifyCompleted() {
        if (completedListener != null) {
            mainHandler.post(() -> completedListener.onCompleted(targetFile));
        }
    }

    private void notifyError(Exception e) {
        state = DownloadState.ERROR;
        if (errorListener != null) {
            mainHandler.post(() -> errorListener.onError(e));
        }
    }

    // ========== Callback interfaces ==========

    public interface OnMetadataReceivedListener {
        void onMetadataReceived(FileMetadata metadata);
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
