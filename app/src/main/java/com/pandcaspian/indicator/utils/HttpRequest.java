package com.pandcaspian.indicator.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modern HTTP request handler using ExecutorService.
 * <p>
 * Features:
 * - Fluent API for easy configuration
 * - Support for GET/POST/PUT/DELETE methods
 * - Request headers and body support
 * - Progress tracking for large responses
 * - Cancellation support
 * - Thread-safe callbacks on main thread
 * - Binary data collection support
 */
public class HttpRequest {

    private static final String TAG = "HttpRequest";
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    private static final int DEFAULT_READ_TIMEOUT = 15000;

    // Shared executor for all HTTP requests
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Request configuration
    private String url;
    private String method = "GET";
    private final Map<String, String> headers = new HashMap<>();
    private String requestBody;
    private String contentType = "application/json";
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private boolean followRedirects = true;
    private boolean collectAsBinary = false;

    // State management
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private Future<?> requestFuture;
    private HttpURLConnection connection;

    // Response data
    private int responseCode;
    private String responseMessage;
    private Map<String, String> responseHeaders;
    private byte[] binaryData;
    private LocalDateTime serverDateTime;
    private String serverName;

    // Callbacks
    private OnStartListener onStart;
    private OnSuccessListener onSuccess;
    private OnBinarySuccessListener onBinarySuccess;
    private OnErrorListener onError;
    private OnCompleteListener onComplete;
    private OnProgressListener onProgress;
    private OnDateTimeReceivedListener onDateTimeReceived;

    public HttpRequest() {
    }

    // ========== Fluent Configuration API ==========

    public HttpRequest url(@NonNull String url) {
        this.url = url;
        return this;
    }

    public HttpRequest method(@NonNull String method) {
        this.method = method.toUpperCase();
        return this;
    }

    public HttpRequest get(@NonNull String url) {
        this.url = url;
        this.method = "GET";
        return this;
    }

    public HttpRequest post(@NonNull String url) {
        this.url = url;
        this.method = "POST";
        return this;
    }

    public HttpRequest header(@NonNull String name, @NonNull String value) {
        headers.put(name, value);
        return this;
    }

    public HttpRequest body(@Nullable String body) {
        this.requestBody = body;
        return this;
    }

    public HttpRequest contentType(@NonNull String contentType) {
        this.contentType = contentType;
        return this;
    }

    public HttpRequest connectTimeout(int millis) {
        this.connectTimeout = millis;
        return this;
    }

    public HttpRequest readTimeout(int millis) {
        this.readTimeout = millis;
        return this;
    }

    public HttpRequest followRedirects(boolean follow) {
        this.followRedirects = follow;
        return this;
    }

    public HttpRequest collectAsBinary(boolean binary) {
        this.collectAsBinary = binary;
        return this;
    }

    // ========== Callback Configuration ==========

    public HttpRequest onStart(OnStartListener listener) {
        this.onStart = listener;
        return this;
    }

    public HttpRequest onSuccess(OnSuccessListener listener) {
        this.onSuccess = listener;
        return this;
    }

    public HttpRequest onBinarySuccess(OnBinarySuccessListener listener) {
        this.onBinarySuccess = listener;
        return this;
    }

    public HttpRequest onError(OnErrorListener listener) {
        this.onError = listener;
        return this;
    }

    public HttpRequest onComplete(OnCompleteListener listener) {
        this.onComplete = listener;
        return this;
    }

    public HttpRequest onProgress(OnProgressListener listener) {
        this.onProgress = listener;
        return this;
    }

    public HttpRequest onDateTimeReceived(OnDateTimeReceivedListener listener) {
        this.onDateTimeReceived = listener;
        return this;
    }

    // ========== Execution ==========

    /**
     * Execute the request asynchronously
     */
    public HttpRequest execute() {
        if (url == null || url.isEmpty()) {
            notifyError(new IllegalStateException("URL not set"), 0);
            return this;
        }

        cancelled.set(false);
        notifyStart();

        requestFuture = executor.submit(this::performRequest);
        return this;
    }

    /**
     * Cancel the request
     */
    public void cancel() {
        cancelled.set(true);
        if (requestFuture != null) {
            requestFuture.cancel(true);
        }
        if (connection != null) {
            connection.disconnect();
        }
    }

    /**
     * Check if request is cancelled
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    // ========== Response Accessors ==========

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public byte[] getBinaryData() {
        return binaryData;
    }

    public LocalDateTime getServerDateTime() {
        return serverDateTime;
    }

    public String getServerName() {
        return serverName;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }

    public String getUrl() {
        return url;
    }

    // ========== Private Implementation ==========

    private void performRequest() {
        try {
            if (cancelled.get()) return;

            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setInstanceFollowRedirects(followRedirects);

            // Set headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Set body if present
            if (requestBody != null && !requestBody.isEmpty()) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", contentType);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            responseCode = connection.getResponseCode();
            responseMessage = connection.getResponseMessage();

            // Parse response headers
            parseResponseHeaders();

            // Parse date header for server time
            parseDateHeader();

            // Handle error responses
            if (responseCode >= 400) {
                notifyError(new HttpException(responseCode, responseMessage, url), responseCode);
                return;
            }

            // Read response
            if (collectAsBinary) {
                binaryData = readBinaryResponse();
                notifyBinarySuccess(binaryData);
            } else {
                String response = readTextResponse();
                notifySuccess(response);
            }

        } catch (Exception e) {
            Log.e(TAG, "Request failed", e);
            notifyError(e, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            notifyComplete();
        }
    }

    private void parseResponseHeaders() {
        responseHeaders = new HashMap<>();
        for (int i = 0; ; i++) {
            String name = connection.getHeaderFieldKey(i);
            String value = connection.getHeaderField(i);
            if (name == null && value == null) break;
            if (name != null) {
                responseHeaders.put(name, value);
            }
        }
    }

    private void parseDateHeader() {
        String dateHeader = connection.getHeaderField("Date");
        if (dateHeader != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateHeader, formatter);
                serverDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                serverName = connection.getHeaderField("Server");

                if (onDateTimeReceived != null) {
                    final LocalDateTime dt = serverDateTime;
                    final String sn = serverName;
                    mainHandler.post(() -> onDateTimeReceived.onDateTimeReceived(dt, sn));
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing date header", e);
            }
        }
    }

    private String readTextResponse() throws IOException {
        InputStream stream = responseCode < 400 ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) return "";
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (cancelled.get()) break;
            response.append(line).append("\n");
        }
        reader.close();
        return response.toString();
    }

    private byte[] readBinaryResponse() throws IOException {
        InputStream stream = new BufferedInputStream(connection.getInputStream());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        long contentLength = connection.getContentLengthLong();
        long totalRead = 0;
        int bytesRead;
        byte[] data = new byte[8192];

        while ((bytesRead = stream.read(data, 0, data.length)) != -1) {
            if (cancelled.get()) break;
            buffer.write(data, 0, bytesRead);
            totalRead += bytesRead;

            if (onProgress != null && contentLength > 0) {
                final long read = totalRead;
                final long total = contentLength;
                mainHandler.post(() -> onProgress.onProgress(read, total));
            }
        }

        stream.close();
        return buffer.toByteArray();
    }

    // ========== Notification Methods ==========

    private void notifyStart() {
        if (onStart != null) {
            mainHandler.post(() -> onStart.onStart());
        }
    }

    private void notifySuccess(String response) {
        if (onSuccess != null && response != null) {
            mainHandler.post(() -> onSuccess.onSuccess(response, responseCode));
        }
    }

    private void notifyBinarySuccess(byte[] data) {
        if (onBinarySuccess != null && data != null) {
            mainHandler.post(() -> onBinarySuccess.onSuccess(data, responseCode));
        }
    }

    private void notifyError(Exception e, int statusCode) {
        if (onError != null) {
            mainHandler.post(() -> onError.onError(e, statusCode, url));
        }
    }

    private void notifyComplete() {
        if (onComplete != null) {
            mainHandler.post(() -> onComplete.onComplete());
        }
    }

    // ========== Callback Interfaces ==========

    public interface OnStartListener {
        void onStart();
    }

    public interface OnSuccessListener {
        void onSuccess(String response, int statusCode);
    }

    public interface OnBinarySuccessListener {
        void onSuccess(byte[] data, int statusCode);
    }

    public interface OnErrorListener {
        void onError(Exception e, int statusCode, String url);
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    public interface OnProgressListener {
        void onProgress(long bytesRead, long totalBytes);
    }

    public interface OnDateTimeReceivedListener {
        void onDateTimeReceived(LocalDateTime dateTime, String serverName);
    }

    // ========== Exception Class ==========

    public static class HttpException extends IOException {
        private final int statusCode;
        private final String statusMessage;
        private final String requestUrl;

        public HttpException(int statusCode, String statusMessage, String url) {
            super("HTTP " + statusCode + ": " + statusMessage);
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.requestUrl = url;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public String getRequestUrl() {
            return requestUrl;
        }
    }

    // ========== Static Utilities ==========

    /**
     * Shutdown the shared executor (call when app is destroyed)
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
