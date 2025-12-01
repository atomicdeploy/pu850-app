package com.pandcaspian.indicator.utils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.pandcaspian.indicator.MainActivity;
import com.pandcaspian.indicator.R;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @deprecated Use {@link ModernHttpRequest} instead, which uses ExecutorService
 * and provides a modern fluent API with better error handling.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class HttpRequest extends AsyncTask<String, Integer, String> {

	private final MainActivity context;
	ProgressDialog progressDialog;

	private static final String TAG = "HttpRequest";
	public boolean inBackground = false, showProgress = false;
	public boolean collectAsBulk = false;

	public HttpURLConnection urlConnection = null;
	public String apiUrl; // Url of api

	// private JSONObject postData; // JSON body of the post

	// Constructor
	public HttpRequest(MainActivity _ctx) {
		context = _ctx;
	}

	// public void setPostData(/*Map<String, String>*/JSONObject postData)
	// {
	// 	if (postData != null) {
	// 		this.postData = postData; // new JSONObject(postData);
	// 	}
	// }

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();

		if ((context.isPerformingRequest || context.lastReadStatus == 1) && !inBackground)
		{
			if (context.lastReadStatus != 1)
			{
				Toast toast = context.makeToast(context, R.string.str_another_request_already_performing, R.drawable.ic_icon_wait, Toast.LENGTH_LONG);
				toast.show();
			}
			else
			{
				context.displayResult(context.getString(R.string.str_operation_not_permitted));
			}

			context.refreshConnectButton();

			cancel(true);

			return;
		}

		if (context.isFinishing())
		{
			cancel(true);
			return;
		}

		if (!inBackground) {
			context.isPerformingRequest = true;
		}

		context.refreshConnectButton();

		// create a progress dialog to show the user what is happening
		if (showProgress) {
			progressDialog = new ProgressDialog(new ContextThemeWrapper(context, R.style.ProgressDialogStyle));
			// progressDialog.setTitle("PU850 request to ESP");
			progressDialog.setMessage(context.getString(R.string.str_performing_request));
			progressDialog.setCancelable(true);
			progressDialog.setIndeterminate(true);
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface dialog)
				{
					// cancel AsyncTask
					HttpRequest.this.cancel(true);
				}
			});

			/*
			progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ctx.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					progressDialog.cancel();
				}
			});
			*/

			progressDialog.getWindow().setBackgroundDrawable(context.getResources().getDrawable(R.drawable.rounded_dialog));

			progressDialog.show();
		}
	}

	// This is a function that we are overriding from AsyncTask.
	// It takes Strings as parameters because that is what is defined for the parameters of the async task
	@Override
	protected String doInBackground(String... params) {

		if (isCancelled()) return null;

		Log.v(TAG, params[0]);

		apiUrl = params[0];

		try {

			String METHOD = "GET";

			URL url = new URL(params[0]);

			if (params.length > 1 && !params[1].isEmpty()) METHOD = params[1];

			// Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.1.30", 8888));

			// Create the urlConnection
			urlConnection = (HttpURLConnection) url.openConnection(/*proxy*/);

			urlConnection.setConnectTimeout(10 * 1000);
			urlConnection.setReadTimeout(10 * 1000);
			urlConnection.setInstanceFollowRedirects(true);

			urlConnection.setRequestMethod(METHOD);
			// urlConnection.setDoInput(true);
			// urlConnection.setDoOutput(true); // to make POST request

			// urlConnection.setRequestProperty("Content-Type", "application/json");
			// urlConnection.addRequestProperty("Accept", "application/json");

			// Credentials
			// urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64("user123:p@ssw0rd").toString());

			// Send the POST body
			/*
			if (this.postData != null) {
				OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
				writer.write(postData.toString());
				writer.flush();
				// readStream(in);
			}
			*/

			final int statusCode = urlConnection.getResponseCode();

			String responseMessage = urlConnection.getResponseMessage();

			String dateHeader = urlConnection.getHeaderField("Date");

			if (dateHeader != null) {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
					DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
					ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateHeader, formatter);
					LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
					context.onDateTimeReceived(localDateTime, urlConnection.getHeaderField("Server"));
				}
			}

			if (statusCode != 200)
			{
				context.onHttpError(statusCode, responseMessage, params[0], this);
				// ??? getErrorStream();
				return null;
			}

			if (collectAsBulk)
			{
				InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[2048];

				while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				context.BulkData = buffer.toByteArray();

				return buffer.toString();
			}
			else
			{
				// String response = convertInputStreamToString(inputStream);

				BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

				final long contentLength = urlConnection.getContentLengthLong();

				String line;
				StringBuilder content = new StringBuilder();

				while ((line = rd.readLine()) != null) {
					content.append(line).append("\n");
				}

				return content.toString();
			}

		} catch (final Exception e) {
			// ConnectException, MalformedURLException, ProtocolException, IOException
			Log.w(TAG, context.getString(R.string.str_failure_to_perform_http_request), e);
			e.printStackTrace();

			context.onHttpError(e, this);

			return null;
		}

		finally
		{
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}

	}

	protected void onProgressUpdate(Integer... progress)
	{

	}

	protected void onPostExecute(String result)
	{
		// dismiss the progress dialog after receiving data from API
		if (progressDialog != null)
			progressDialog.dismiss();

		context.isPerformingRequest = false;

		context.refreshConnectButton();

		if (isCancelled()) return;

		try
		{
			// JSONObject jsonObject = new JSONObject(result);

			context.updateResults(apiUrl, result);

			/*
			// String result = jsonObject.getString("message")
			JSONArray jsonData = jsonObject.getJSONArray("results");
			JSONObject jsonObject = jsonData.getJSONObject(index_no);
			String id = jsonObject.getString("id");
			String name = jsonObject.getString("name");
			*/
		}

		catch (Exception e)
		{
			e.printStackTrace();

			// Toast toast = Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG);
			// ctx.setToastStyle(toast);
			// toast.show();
		}
	}

	protected void onCancelled()
	{
		if (progressDialog != null)
			progressDialog.dismiss();

		if (urlConnection != null)
			urlConnection.disconnect();

		context.isPerformingRequest = false;

		context.refreshConnectButton();

		// if (context.lastReadStatus == 1)
		// 	return;
	}
}
