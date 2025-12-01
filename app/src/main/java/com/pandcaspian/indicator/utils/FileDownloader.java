package com.pandcaspian.indicator.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.pandcaspian.indicator.MainActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @deprecated Use {@link ModernFileDownloader} instead, which uses ExecutorService
 * and supports resume/pause, metadata checking, and better error handling.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class FileDownloader extends AsyncTask<String, String, String> {

	// private static final int TIMEOUT = 1000;

	private final MainActivity context;
	private PowerManager.WakeLock mWakeLock;

	public OnFileInfoReceived onFileInfoReceived;
	public OnProgressUpdate onProgressUpdate;
	public OnCompleted onCompleted;

	private String fileUrl;
	private File mTargetFile;

	private boolean isCancelled = false;
	private int lastProgress = -1;

	/**
	 * Constructor parameters:
	 * @context (current Activity)
	 * @fileUrl (URL to download file)
	 * @targetFile (File object to write, it will be overwritten if exist)
	 */
	public FileDownloader(MainActivity context, String fileUrl, File targetFile) {
		this.context = context;
		this.fileUrl = fileUrl;
		this.mTargetFile = targetFile;
	}

	/**
	 * Before starting background thread
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		// take CPU lock to prevent CPU from going off if the user presses the power button to turn the screen off during download
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
	}

	/**
	 * Download file in the background thread
	 */
	@Override
	// protected Boolean doInBackground(String... params) {
	protected String doInBackground(String... params) {
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection urlConnection = null;

		try {
			URL url = new URL(fileUrl);

			// Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.1.30", 8888));
			urlConnection = (HttpURLConnection) url.openConnection(/*proxy*/);

			urlConnection.setConnectTimeout(10000);
			urlConnection.setReadTimeout(5000);

			urlConnection.setInstanceFollowRedirects(true);

			urlConnection.setRequestMethod("GET");

			// urlConnection.setRequestMethod("HEAD");

			// boolean rangeSupport = urlConnection.getHeaderField("Accept-Ranges").equals("bytes");

			// long existingFileSize = outputFile.length();
			// if (existingFileSize < fileLength) {
			// 	httpFileConnection.setRequestProperty("Range", "bytes=" + existingFileSize + "-" + fileLength);
			// }

			// if (params.length > 0) {
			//     String rangeHeader = "bytes=" + params[0] + "-";
			//     urlConnection.setRequestProperty("Range", rangeHeader);
			// }

			// expectedStatusCode = HttpStatus.SC_PARTIAL_CONTENT;

			urlConnection.connect();

			// expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
			if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return "Server returned HTTP " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage();

			final String contentDisposition = urlConnection.getHeaderField("Content-Disposition");

			String fileName = URLUtil.guessFileName(String.valueOf(url), contentDisposition, null);

			// try to get file name from content disposition
			if (!contentDisposition.isEmpty()) {
				String extracted = contentDisposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
				if (!extracted.isEmpty() && !extracted.equals(contentDisposition)) {
					fileName = extracted;
				}
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				fileName = URLDecoder.decode(fileName, StandardCharsets.ISO_8859_1);
			} else {
				fileName = URLDecoder.decode(fileName, "ISO-8859-1");
			}

			// get the file date from the last modified header
			final String lastModified = urlConnection.getHeaderField("Last-Modified");

			if (!lastModified.isEmpty()) {
				Date lastModifiedDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(lastModified);
			}

			// get the file size from content-length header, used to calculate download percentage
			// might be -1: server did not report the length
			final long fileLength = urlConnection.getContentLengthLong();

			if (onFileInfoReceived != null) {
				onFileInfoReceived.onFileInfoReceived(this, fileLength, fileName);
			}

			// input stream to read file
			input = new BufferedInputStream(urlConnection.getInputStream(), 1024);

			// output stream to write file
			output = new FileOutputStream(mTargetFile, false); // false = overwrite, true = append

			// FileChannel ch = ((FileOutputStream) output).getChannel();
			// ch.position(offset);
			// ch.write(ByteBuffer.wrap(data));

			byte[] data = new byte[1024];
			long total = 0;
			int count;

			while (true) {
				// check if cancelled requested
				if (isCancelled() || isCancelled) {
					isCancelled = true;
					return "Download cancelled";
				}

				try {
					// read data from input stream
					if ((count = input.read(data)) == -1) break;
				}
				catch (IOException e) {
					return "Download Stopped";
				}

				total += count;

				if (fileLength > 0) // only if total length is known
				{
					// After this onProgressUpdate will be called
					publishProgress(String.valueOf((int) ((total * 100) / fileLength)), String.valueOf(total));
				}

				// write received data to file
				output.write(data, 0, count);
			}

			// flushing output
			output.flush();
		} catch (Exception e) {
			Log.e("FileDownloader", e.getMessage());
			e.printStackTrace();
			return e.getMessage() + " by " + e.getClass().getName();
			// onDownloadError() - e.g. file error
		} finally {

			// closing streams
			try {
				if (input != null) input.close();
			} catch (IOException ignored) {}

			try {
				if (output != null) output.close();
			} catch (IOException ignored) {}

			// close urlConnection
			if (urlConnection != null)
				urlConnection.disconnect();
		}

		return null;
	}

	/**
	 * Update progress information
	 */
	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);

		final int progress = Integer.parseInt(values[0]);

		if (progress != lastProgress) {
			lastProgress = progress;
			Log.d("FileDownloader", String.format(Locale.ENGLISH, "Download Progress: %d%%", progress));
		}

		final int total = Integer.parseInt(values[1]);

		if (onProgressUpdate != null) {
			onProgressUpdate.onProgressUpdate(this, total);
		}

	}

	/**
	 * After completing background task
	 */
	@Override
	// protected void onPostExecute(Boolean result) {
	protected void onPostExecute(String result) {
		Log.i("FileDownloader", "Download PostExecute");

		if (mWakeLock.isHeld())
			mWakeLock.release();

		if (onCompleted != null)
			onCompleted.onCompleted(this);

		if (result != null) {
			Log.e("FileDownloader", result);
			Toast.makeText(context, String.format("Download error: %s", result), Toast.LENGTH_LONG).show();
			return;
		}

		Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onCancelled() {
		if (mWakeLock.isHeld())
			mWakeLock.release();

		if (onCompleted != null)
			onCompleted.onCompleted(this);

		isCancelled = true;
	}

	public void cancel() {
		isCancelled = true;
		super.cancel(true);
	}

	public interface OnFileInfoReceived {
		void onFileInfoReceived(FileDownloader downloadTask, long fileSize, String fileName);
	}

	public interface OnProgressUpdate {
		void onProgressUpdate(FileDownloader downloadTask, int progress);
	}

	public interface OnCompleted {
		void onCompleted(FileDownloader downloadTask);

		void onCompleted(FileDownloader downloadTask, File outputFile);
	}
}
