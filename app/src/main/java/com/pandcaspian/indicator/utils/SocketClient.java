package com.pandcaspian.indicator.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.pandcaspian.indicator.MainActivity;
import com.pandcaspian.indicator.R;

import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class SocketClient extends WebSocketClient {

	MainActivity ctx;

	AlertDialog progressDialog;

	public SocketClient(URI serverURI, MainActivity _ctx) {
		super(serverURI);

		ctx = _ctx;

		setConnectionLostTimeout(10);

		// Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.1.30", 8888));
		// setProxy(proxy);

		// Create modern progress dialog using AlertDialog
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.ProgressDialogStyle);
		
		// Create custom view with progress indicator
		View dialogView = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_1, null);
		TextView textView = dialogView.findViewById(android.R.id.text1);
		textView.setText(ctx.getString(R.string.str_wait_until_operation_finished));
		textView.setPadding(48, 48, 48, 48);
		
		builder.setTitle(ctx.getString(R.string.str_connecting_to_device));
		builder.setView(dialogView);
		builder.setIcon(R.drawable.ic_icon_wifi);
		builder.setCancelable(true);
		builder.setNegativeButton(ctx.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				closeConnection(0, "");
				dialog.dismiss();
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				closeConnection(0, "");
			}
		});

		progressDialog = builder.create();
		if (progressDialog.getWindow() != null) {
			progressDialog.getWindow().setBackgroundDrawable(
				ContextCompat.getDrawable(ctx, R.drawable.rounded_dialog)
			);
		}

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