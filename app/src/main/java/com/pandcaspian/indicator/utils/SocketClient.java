package com.pandcaspian.indicator.utils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.res.ResourcesCompat;

import com.pandcaspian.indicator.MainActivity;
import com.pandcaspian.indicator.R;

import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class SocketClient extends WebSocketClient {

	MainActivity ctx;

	ProgressDialog progressDialog;

	public SocketClient(URI serverURI, MainActivity _ctx) {
		super(serverURI);

		ctx = _ctx;

		setConnectionLostTimeout(10);

		// Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.1.30", 8888));
		// setProxy(proxy);

		progressDialog = new ProgressDialog(new ContextThemeWrapper(ctx, R.style.ProgressDialogStyle));
		progressDialog.setTitle(ctx.getString(R.string.str_connecting_to_device));
		progressDialog.setMessage(ctx.getString(R.string.str_wait_until_operation_finished));
		progressDialog.setCancelable(true);
		progressDialog.setIndeterminate(true);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		// progressDialog.setIndeterminateDrawable(ctx.getResources().getDrawable(R.drawable.ic_icon_wait));
		progressDialog.setIcon(R.drawable.ic_icon_wifi);
		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog) {
				// ctx.onWsError(new Exception(""));
				closeConnection(0, ""); // close(0, "");
				progressDialog.dismiss();
			}
		});

		progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, ctx.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				progressDialog.cancel();
			}
		});

		progressDialog.getWindow().setBackgroundDrawable(ctx.getResources().getDrawable(R.drawable.rounded_dialog));

		progressDialog.show();
	}

	public SocketClient(String serverURI, MainActivity _ctx) {
		// The `dnsResolver` method is already built into the class, use that to resolve IP later
		this(URI.create(serverURI), _ctx);
	}

	@Override
	public void onOpen(ServerHandshake handshakeData) {
		Log.i("WebSocketClient", "new websocket connection opened");
		// Send WebSocket message
		// send("client:helloWorld");

		send("weight");

		if (progressDialog != null) progressDialog.dismiss();

		ctx.onWsConnected();
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		Log.e("WebSocketClient", String.format("closed websocket with exit code %d additional info: %s", code, reason));
		if (progressDialog != null) progressDialog.dismiss();
		ctx.onWsDisconnected(code, reason, remote);
	}

	@Override
	public void onMessage(String message) {
		Log.d("WebSocketClient", String.format("received message: %s", message));
		ctx.onWsUpdate(message);
	}

	@Override
	public void onMessage(ByteBuffer message) {
		Log.d("WebSocketClient", "received ByteBuffer");
		// (TODO)
	}

	@Override
	public void onError(Exception e) {
		if (progressDialog != null) progressDialog.dismiss();
		ctx.onWsError(e);

		e.printStackTrace();
	}

}