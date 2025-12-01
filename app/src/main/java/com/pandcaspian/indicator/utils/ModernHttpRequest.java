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
 * Modern HTTP request handler using ExecutorService instead of deprecated AsyncTask.
 * Features:
 * - Fluent API for easy configuration
 * - Support for GET/POST/PUT/DELETE methods
 * - Request headers support
 * - Request body support (JSON, form data)
 * - Progress tracking for large responses
 * - Cancellation support
 * - Thread-safe callbacks on main thread
 */
public class ModernHttpRequest {

    private static final String TAG = "ModernHttpRequest";
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    private static final int DEFAULT_READ_TIMEOUT = 15000;

    // Shared executor for all HTTP requests
    private static final ExecutorService sharedExecutor = Executors.newFixedThreadPool(4);

    private final Context context;
    private final Handler mainHandler;

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
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private Future<?> requestFuture;

    // Callbacks
    private OnStartListener startListener;
    private OnSuccessListener successListener;
    private OnBinarySuccessListener binarySuccessListener;
    private OnErrorListener errorListener;
    private OnCompleteListener completeListener;
    private OnProgressListener progressListener;
    private OnHeadersReceivedListener headersReceivedListener;

    // Response data
    private int responseCode;
    private String responseMessage;
    private Map<String, String> responseHeaders;
    private LocalDateTime serverDateTime;
    private String serverName;

    public ModernHttpRequest(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // ========== Fluent API Configuration ==========

    /**
     * Set the request URL
     */
    public ModernHttpRequest setUrl(@NonNull String url) {
        this.url = url;
        return this;
    }

    /**
     * Set the HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    public ModernHttpRequest setMethod(@NonNull String method) {
        this.method = method.toUpperCase();
        return this;
    }

    /**
     * Convenience method for GET request
     */
    public ModernHttpRequest get(@NonNull String url) {
        this.url = url;
        this.method = "GET";
        return this;
    }

    /**
     * Convenience method for POST request
     */
    public ModernHttpRequest post(@NonNull String url) {
        this.url = url;
        this.method = "POST";
        return this;
    }

    /**
     * Add a header to the request
     */
    public ModernHttpRequest addHeader(@NonNull String name, @NonNull String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Set the request body
     */
    public ModernHttpRequest setBody(@Nullable String body) {
        this.requestBody = body;
        return this;
    }

    /**
     * Set the content type for the request body
     */
    public ModernHttpRequest setContentType(@NonNull String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Set connection timeout in milliseconds
     */
    public ModernHttpRequest setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    /**
     * Set read timeout in milliseconds
     */
    public ModernHttpRequest setReadTimeout(int timeout) {
        this.readTimeout = timeout;
        return this;
    }

    /**
     * Set whether to follow redirects
     */
    public ModernHttpRequest setFollowRedirects(boolean follow) {
        this.followRedirects = follow;
        return this;
    }

    /**
     * Set whether to collect response as binary data
     */
    public ModernHttpRequest setCollectAsBinary(boolean binary) {
        this.collectAsBinary = binary;
        return this;
    }

    // ========== Callback Configuration ==========

    /**
     * Called when request starts
     */
    public ModernHttpRequest setOnStart(OnStartListener listener) {
        this.startListener = listener;
        return this;
    }

    /**
     * Called on successful response (text)
     */
    public ModernHttpRequest setOnSuccess(OnSuccessListener listener) {
        this.successListener = listener;
        return this;
    }

    /**
     * Called on successful response (binary)
     */
    public ModernHttpRequest setOnBinarySuccess(OnBinarySuccessListener listener) {
        this.binarySuccessListener = listener;
        return this;
    }

    /**
     * Called on error
     */
    public ModernHttpRequest setOnError(OnErrorListener listener) {
        this.errorListener = listener;
        return this;
    }

    /**
     * Called when request completes (success or error)
     */
    public ModernHttpRequest setOnComplete(OnCompleteListener listener) {
        this.completeListener = listener;
        return this;
    }

    /**
     * Called for progress updates during large downloads
     */
    public ModernHttpRequest setOnProgress(OnProgressListener listener) {
        this.progressListener = listener;
        return this;
    }

    /**
     * Called when response headers are received
     */
    public ModernHttpRequest setOnHeadersReceived(OnHeadersReceivedListener listener) {
        this.headersReceivedListener = listener;
        return this;
    }

    // ========== Execution ==========

    /**
     * Execute the request asynchronously
     */
    public ModernHttpRequest execute() {
        if (url == null || url.isEmpty()) {
            notifyError(new IllegalStateException("URL not set"));
            return this;
        }

        isCancelled.set(false);
        notifyStart();

        requestFuture = sharedExecutor.submit(this::doRequest);
        return this;
    }

    /**
     * Execute the request synchronously (blocks current thread)
     * @return Response as string, or null on error
     */
    @Nullable
    public String executeSync() throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("URL not set");
        }

        HttpURLConnection connection = null;
        try {
            connection = createConnection();
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Cancel the request
     */
    public void cancel() {
        isCancelled.set(true);
        if (requestFuture != null) {
            requestFuture.cancel(true);
        }
    }

    /**
     * Check if request is cancelled
     */
    public boolean isCancelled() {
        return isCancelled.get();
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

    public LocalDateTime getServerDateTime() {
        return serverDateTime;
    }

    public String getServerName() {
        return serverName;
    }

    // ========== Private Implementation ==========

    private void doRequest() {
        HttpURLConnection connection = null;
        try {
            if (isCancelled.get()) {
                return;
            }

            connection = createConnection();

            // Read response
            if (collectAsBinary) {
                byte[] data = readBinaryResponse(connection);
                notifyBinarySuccess(data);
            } else {
                String response = readResponse(connection);
                notifySuccess(response);
            }

        } catch (Exception e) {
            Log.e(TAG, "Request failed", e);
            notifyError(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            notifyComplete();
        }
    }

    private HttpURLConnection createConnection() throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

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
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        // Get response code and message
        responseCode = connection.getResponseCode();
        responseMessage = connection.getResponseMessage();

        // Parse response headers
        parseResponseHeaders(connection);

        // Check for error status
        if (responseCode >= 400) {
            throw new HttpException(responseCode, responseMessage, url);
        }

        return connection;
    }

    private void parseResponseHeaders(HttpURLConnection connection) {
        responseHeaders = new HashMap<>();

        // Get all headers
        for (int i = 0; ; i++) {
            String headerName = connection.getHeaderFieldKey(i);
            String headerValue = connection.getHeaderField(i);
            if (headerName == null && headerValue == null) break;
            if (headerName != null) {
                responseHeaders.put(headerName, headerValue);
            }
        }

        // Parse Date header for server time
        String dateHeader = connection.getHeaderField("Date");
        if (dateHeader != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateHeader, formatter);
                serverDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception e) {
                Log.w(TAG, "Error parsing date header", e);
            }
        }

        serverName = connection.getHeaderField("Server");

        // Notify headers received
        if (headersReceivedListener != null) {
            final Map<String, String> finalHeaders = new HashMap<>(responseHeaders);
            mainHandler.post(() -> headersReceivedListener.onHeadersReceived(finalHeaders, responseCode));
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode < 400 ? connection.getInputStream() : connection.getErrorStream()));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (isCancelled.get()) {
                reader.close();
                return null;
            }
            response.append(line).append("\n");
        }
        reader.close();

        return response.toString();
    }

    private byte[] readBinaryResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        long contentLength = connection.getContentLengthLong();
        long totalRead = 0;
        int bytesRead;
        byte[] data = new byte[8192];

        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            if (isCancelled.get()) {
                inputStream.close();
                return null;
            }
            buffer.write(data, 0, bytesRead);
            totalRead += bytesRead;

            // Report progress
            if (progressListener != null && contentLength > 0) {
                final long finalTotalRead = totalRead;
                final long finalContentLength = contentLength;
                mainHandler.post(() -> progressListener.onProgress(finalTotalRead, finalContentLength));
            }
        }

        inputStream.close();
        return buffer.toByteArray();
    }

    // ========== Notification Methods ==========

    private void notifyStart() {
        if (startListener != null) {
            mainHandler.post(() -> startListener.onStart());
        }
    }

    private void notifySuccess(String response) {
        if (successListener != null && response != null) {
            mainHandler.post(() -> successListener.onSuccess(response, responseCode));
        }
    }

    private void notifyBinarySuccess(byte[] data) {
        if (binarySuccessListener != null && data != null) {
            mainHandler.post(() -> binarySuccessListener.onSuccess(data, responseCode));
        }
    }

    private void notifyError(Exception e) {
        if (errorListener != null) {
            mainHandler.post(() -> errorListener.onError(e, responseCode, url));
        }
    }

    private void notifyComplete() {
        if (completeListener != null) {
            mainHandler.post(() -> completeListener.onComplete());
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

    public interface OnHeadersReceivedListener {
        void onHeadersReceived(Map<String, String> headers, int statusCode);
    }

    // ========== Exception Classes ==========

    /**
     * Exception for HTTP error responses
     */
    public static class HttpException extends IOException {
        private final int statusCode;
        private final String statusMessage;
        private final String url;

        public HttpException(int statusCode, String statusMessage, String url) {
            super("HTTP " + statusCode + ": " + statusMessage);
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.url = url;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public String getUrl() {
            return url;
        }
    }

    // ========== Static Utilities ==========

    /**
     * Shutdown the shared executor (call when app is destroyed)
     */
    public static void shutdown() {
        sharedExecutor.shutdown();
    }
}
