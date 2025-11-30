package com.pandcaspian.indicator;

import static androidx.core.app.NotificationCompat.DEFAULT_SOUND;
import static androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.service.notification.StatusBarNotification;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pandcaspian.indicator.utils.FileDownloader;
import com.pandcaspian.indicator.utils.HttpRequest;
import com.pandcaspian.indicator.utils.SocketClient;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.hamsaa.persiandatepicker.PersianDatePickerDialog;
import ir.hamsaa.persiandatepicker.api.PersianPickerDate;
import ir.hamsaa.persiandatepicker.api.PersianPickerListener;

public class MainActivity extends AppCompatActivity {

	final int[] fromDate = { 0, 0, 0 }, toDate = { 0, 0, 0 };

	final int[] PrintPaperInfo = { -1, -1 };

	long lastWsUpdate = 0, lastAvailCheck = 0;

	private int accessLevel = 0;
	private String currentPassword = "";

	String SystemLanguage = "";
	boolean isLanguageReceivedFromPu = false;

	int yearPU = 0, monthPU = 0, dayPU = 0;
	int hourPU = 0, minutePU = 0, secondPU = 0;

	final byte TotalCharAnyLinePrint_ = 75 - 2;
	final int fontHeight = 14;
	final int fontBytes = 1;
	final int fontFileSize = 12 * 15 * fontHeight * fontBytes;
	byte[] dataFont = new byte[fontFileSize];
	public byte[] BulkData = new byte[TotalCharAnyLinePrint_ * 100];
	private Ringtone ringtone;
	private AlertDialog dialogSetTime, mDialog;
	long DeviceClockDiff = -1;

	void convertToShamsi(int realYear, int realMonth, int realDay, int[] convertDate) {
		final int[] mon = new int[]{0, 10, 11, 9, 11, 10, 10, 9, 9, 9, 8, 9, 9};

		boolean gotoYearCalc = false;

		if (realMonth == 1 && realDay == 1) {
			if (realYear % 4 == 0) {
				if (realYear % 100 != 0 || (realYear % 100 == 0 && realYear % 400 == 0)) {
					convertDate[0] = realYear - 622; // year
					convertDate[1] = 10; // month
					convertDate[2] = 11; // day
					return;
				}
			}
		}

		if (realYear % 4 == 0 & realMonth > 2)
			realDay++; // Gregorian leap year: increase realDay
		if (realYear % 4 == 1 && realMonth == 3 && realDay == 20) {
			realDay = 30;
			realMonth = 12;
			gotoYearCalc = true;
		}

		if (!gotoYearCalc)
		{
			// Shamsi leap year
			if (realYear % 4 == 1 && (realMonth < 3 || (realMonth == 3 && realDay < 20))) {
				realDay++; // Gregorian leap year remaining: increase realDay
			}

			realDay = realDay + mon[realMonth]; // Calculate Shamsi realDay
			realMonth += 9; // Calculate Shamsi realMonth
			if (realMonth > 12) realMonth -= 12; // Adjust realMonth

			if (realMonth < 7) {
				if (realDay > 31) { // Adjust Shamsi realDay with related realMonth
					realDay = realDay - 31;
					realMonth++;
				}
				gotoYearCalc = true;
			}

			if (!gotoYearCalc) {
				if (realMonth < 12 & realDay > 30) {
					realDay -= 30;
					realMonth++;
				}

				if (realMonth == 12 & realDay > 29) {
					realDay = realDay - 29;
					realMonth = 1;
				}
			}
		}

		realYear -= 621; // Calculate Shamsi realYear

		if (realYear % 4 == 3) { // Adjust Shamsi realYear
			if (realMonth > 10 || (realMonth == 10 && realDay > 11)) realYear--;
		} else {
			if (realMonth > 10 || (realMonth == 10 && realDay > 10)) realYear--;
		}

		convertDate[0] = realYear;
		convertDate[1] = realMonth;
		convertDate[2] = realDay;
	}

	public char PU850_ASCII_to_Persian(char Code)
	{
		if ((int)Code == 172) Code = 'H';
		if ((int)Code == 170) Code = '#';
		if ((int)Code == 167) Code = '_';

		switch (Code)
		{
			case 'q': return 'ض';
			case 'w': return 'ص';
			case 'e': return 'ث';
			case 'r': return 'ق';
			case 't': return 'ف';
			case 'y': return 'غ';
			case 'u': return 'ع';
			case 'i': return 'ه';
			case 'o': return 'خ';
			case 'p': return 'ح';
			case '[': return 'ج';
			case ']': return 'چ';
			case 'a': return 'ش';
			case 's': return 'س';
			case 'd': return 'ی';
			case 'f': return 'ب';
			case 'g': return 'ل';
			case 'h': return 'ا';
			case 'j': return 'ت';
			case 'k': return 'ن';
			case 'l': return 'م';
			case ';': return 'ک';              //= 138; break;   // ke
			case 'z': return 'ظ';              //= 123; break;
			case 'x': return 'ط';              //= 120; break;
			case 'c': return 'ز';              //= 102; break;
			case 'v': return 'ر';              //= 99; break;
			case 'b': return 'ذ';              //= 96; break;
			case 'n': return 'د';              //= 93; break;
			case 'm': return 'ئ';              //= 164; break;
			case ',': return 'و';              //= 153; break;   // Va
			case '`': return 'پ';              //= 72; break;    // Pe
			case '~': return 'ژ';              //= 105; break;   // Zhe
			case '\'': return 'گ';             //= 141; break;   // ''' Gea
			case '#': return 'ً';               //= 170; Kind = 3; break;        // alef tanvein
			case '$': return 'ء';              //= 168; Kind = 1; break;        // hamze
			case '_': return '_';              //= 167; Kind = 3; break;        // Khat Etesal
			case '.': return '.';              //= 46; Kind = 1; break;         // Noghte
			case '&': return '،';              //= 44; Kind = 1; break;         // Kama
			case '?': return '؟';              //= 63; Kind = 1; break;         // ?
			case 'H': return 'آ';              //= 172; Kind = 1; break;        // alef ba kola
			//case '/': return = 47 ; break;
		}
		return Code;
	}

	public String stringManagement(String str)
	{
		StringBuilder result = new StringBuilder();

		for (int i = 0; i < str.length(); i++) {
			char ch;
			switch (SystemLanguage.charAt(0))
			{
				case 'F': ch = PU850_ASCII_to_Persian(str.charAt(i)); break;
				case 'E': ch = str.charAt(i); break;
				default: ch = ' '; break;
			}

			result.append(ch);
		}

		return result.toString();
	}

	public boolean isTableChar(int num)
	{
		return num == 60 || num == 92 || num == 61 || num == 44 || num == 198 ||
				num == 91 || num == 93 || num == 199 || num == 200 || num == 59 ||
				num == 95 || num == 96;
	}

	public void PrintPaperFontSelect() {
		try {
			InputStream fileIn = getResources().openRawResource(R.raw.asa_printfontmob_8x14);

			BufferedInputStream buf = new BufferedInputStream(fileIn);
			buf.read(dataFont, 0, dataFont.length);
			buf.close();
		} catch (Exception e) {
			Log.e("PrintPaperFontSelect", e.getMessage());
			e.printStackTrace();
		}
	}

	private void drawPrintPaper() {
		int data, Shift, raw;
		int cPointer, gPointer;
		int MaxRow = 21;

		int sizeBulk = BulkData.length;

		for (int i = sizeBulk - 1; i >= 0 && BulkData[i] == 0x20; i--)
			sizeBulk--;

		MaxRow = Math.min(MaxRow, (sizeBulk + TotalCharAnyLinePrint_ - 1) / TotalCharAnyLinePrint_);

		final int paddingVertical = 2 * fontHeight, paddingHorizontal = 4 * fontBytes * 8;
		ImageView imageViewPrintPaper = (ImageView) findViewById(R.id.imageViewPrintPaper);

		Bitmap bmpPrintPaper = Bitmap.createBitmap(TotalCharAnyLinePrint_ * fontBytes * 8 + paddingHorizontal, MaxRow * fontHeight + paddingVertical, Bitmap.Config.ARGB_8888);
		imageViewPrintPaper.setImageBitmap(null);

		int fontColor;

		cPointer = 0;
		for (int yy = 0; yy < MaxRow; yy++) {
			for (int xx = 0; xx < TotalCharAnyLinePrint_; xx++) {
				if (cPointer < sizeBulk) {
					raw = (int) (BulkData[cPointer++] & 0xff);
					if (raw < 0x20 || raw > 201)
						raw = 0x20;

					if (raw != 0x20) {
						gPointer = (raw - 0x20) * fontBytes * fontHeight;
						final boolean isColored = isTableChar(raw);

						for (int y = 0; y < fontHeight; y++) {
							for (int x = 0; x < fontBytes; x++) {

								data = (int) (dataFont[gPointer++] & 0xff);

								Shift = 0x01;

								for (int i = 0; i < 8; i++)
								{
									if ((data & Shift) == 0)
										fontColor = !isColored ? Color.BLACK : Color.GRAY;
									else
										fontColor = Color.TRANSPARENT; // getResources().getColor(R.color.colorDarkPaper);

									bmpPrintPaper.setPixel(i + x * 8 + xx * fontBytes * 8 + paddingHorizontal / 2, y + yy * fontHeight + paddingVertical / 2, fontColor);

									Shift = (byte) (Shift << 1);
								}
							}
						}
					}
				}
			}
		}

		if ((PrintPaperInfo[0] > 0 && PrintPaperInfo[1] > 0) || PrintPaperInfo[0] == 2) {
			// Bitmap imageWithBG = Bitmap.createBitmap(bmpPrintPaper);
			Bitmap imageWithBG = Bitmap.createBitmap(bmpPrintPaper.getWidth(), bmpPrintPaper.getHeight(), bmpPrintPaper.getConfig()); // Create another image the same size
			imageWithBG.eraseColor(Color.WHITE); // set its background to white, or whatever color you want
			Canvas canvas = new Canvas(imageWithBG); // create a canvas to draw on the new image
			canvas.drawBitmap(bmpPrintPaper, 0f, 0f, null); // draw old image on the background

			String Kind = "PrintPaper";

			switch (PrintPaperInfo[0]) {
				case 1:
					Kind = "Receipt";
					break;
				case 2:
					Kind = "Report";
					break;
			}

			String fileName = Kind + "_" + PrintPaperInfo[1];

			if (PrintPaperInfo[0] == 2) {
				Locale locale = Locale.getDefault();
				fileName = String.format(locale, "%s_%04d%02d%02d_%04d%02d%02d", Kind,
					fromDate[0], fromDate[1], fromDate[2],
					toDate[0], toDate[1], toDate[2]);
			}

			saveImage(imageWithBG, getApplicationContext(), "PandCaspian", fileName + ".png");
		} else {
			PrintPaperInfo[0] = -1;
			PrintPaperInfo[1] = -1;
		}

		bmpPrintPaper = rotateBitmap(bmpPrintPaper, 90);

		// imageViewPrintPaper.setImageBitmap(bmpPrintPaper);

		BitmapDrawable drawable = new BitmapDrawable(bmpPrintPaper);
		drawable.setAntiAlias(false);
		drawable.setFilterBitmap(false);
		drawable.setDither(false);
		imageViewPrintPaper.setImageDrawable(drawable);

		ViewGroup.LayoutParams layoutParams = imageViewPrintPaper.getLayoutParams();
		layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
		layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
		imageViewPrintPaper.setVisibility(View.VISIBLE);
		imageViewPrintPaper.bringToFront();
		imageViewPrintPaper.setBackgroundColor(getColor(R.color.colorDarkPaper));
		imageViewPrintPaper.setLayoutParams(layoutParams);
		imageViewPrintPaper.invalidate();
		imageViewPrintPaper.requestLayout();
	}

	public Bitmap rotateBitmap(Bitmap original, float degrees) {
		final int width = original.getWidth();
		final int height = original.getHeight();

		Matrix matrix = new Matrix();
		matrix.preRotate(degrees);

		Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);
		// rotatedBitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(rotatedBitmap);
		// canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
		// canvas.drawBitmap(rotatedBitmap, 5.0f, 0.0f, null);

		return rotatedBitmap;
	}

	private void saveImage(Bitmap bitmap, Context context, String folderName, String fileName) {
		boolean success = false;
		final String savePath = Environment.DIRECTORY_PICTURES + "/" + folderName;
		Uri uri = null;
		try {
			if (android.os.Build.VERSION.SDK_INT >= 29) {
				ContentValues values = contentValues();
				values.put(MediaStore.Images.Media.RELATIVE_PATH, savePath);
				values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
				values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
				values.put(MediaStore.Images.Media.IS_PENDING, true);
				uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				filePath = uri == null ? "" : uri.getPath();
				success = saveImageToStream(bitmap, context.getContentResolver().openOutputStream(uri));
				values.put(MediaStore.Images.Media.IS_PENDING, false);
				context.getContentResolver().update(uri, values, null, null);
			}
			else {
				File directory = new File(Environment.getExternalStorageDirectory().toString() + "/" + savePath);
				if (!directory.exists() && !directory.mkdirs()) {
					onImageSaved(false, null);
					return;
				}
				final File file = new File(directory, fileName);
				try { if (file.exists()) file.delete(); }
				catch (Exception ignored) {}
				filePath = file.getAbsolutePath();
				success = saveImageToStream(bitmap, new FileOutputStream(file));
				ContentValues values = contentValues();
				values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
				uri = Uri.fromFile(file);
				context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			}
		} catch (Exception e) {
			onImageSaved(false, null);
			e.printStackTrace();
			// Toast toast = makeToast(getApplicationContext(), e.getMessage(), R.drawable.ic_icon_error, Toast.LENGTH_LONG);
			// toast.show();
			return;
		}
		if (success) {
			onImageSaved(true, uri);
		}
	}

	private boolean saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
		boolean result = false;
		if (outputStream != null) {
			try {
				result = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
				outputStream.flush();
				outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();

				// Toast toast = makeToast(getApplicationContext(), e.getMessage(), R.drawable.ic_icon_error, Toast.LENGTH_LONG);
				// toast.show();
			}
		}
		return result;
	}

	private String filePath = "";

	private void onImageSaved(boolean success, Uri uri) {
		String Kind = getString(R.string.File);

		switch (PrintPaperInfo[0]) {
			case 1:
				Kind = getString(R.string.Receipt);
				break;
			case 2:
				Kind = getString(R.string.Report);
				break;
		}

		PrintPaperInfo[0] = -1;

		String msg = !success ? String.format(getString(R.string.str_failed_to_save_file), Kind) + "\n" + filePath : "";

		if (!msg.isBlank()) {
			Toast toast = makeToast(getApplicationContext(), msg, R.drawable.ic_icon_error, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
			toast.show();
		}

		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
		} else {
			v.vibrate(100);
		}

		if (!success || uri == null) return;

		afterImageSavedDialog(Kind, uri);

	}

	private void afterImageSavedDialog(String Kind, Uri uri) {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);
				final BottomSheetDialog builder = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetDialogTheme);

				// builder.setIcon(R.drawable.ic_icon_information);
				builder.setTitle(String.format(getString(R.string.str_saved_in_gallery), Kind));

				LinearLayout rootLayout = new LinearLayout(MainActivity.this);
				rootLayout.setOrientation(LinearLayout.VERTICAL);

				int alertDialogPadding = getDimensionFromAttribute(MainActivity.this, R.attr.dialogPreferredPadding, 64);

				rootLayout.setPadding(alertDialogPadding, 64, alertDialogPadding, 64);
				rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT
				));

				// Create a TextView
				TextView textView = new TextView(MainActivity.this);
				textView.setText(getFileName(MainActivity.this, uri));
				textView.setPadding(0, 0, 0, 16);

				textView.setTextColor(getColor(R.color.colorForeground));
				textView.setTextSize(14);
				textView.setTypeface(getTypeface(Typeface.DEFAULT, false), Typeface.NORMAL);

				textView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
				textView.setTextDirection(View.TEXT_DIRECTION_LTR);

				rootLayout.addView(getDialogTitle(builder, String.format(getString(R.string.str_saved_in_gallery), Kind), R.drawable.ic_icon_information));
				rootLayout.addView(textView);

				// Create Button to Open the Image
				Button buttonOpenImage = new Button(MainActivity.this);
				buttonOpenImage.setText(R.string.str_btn_open_in_the_gallery);
				buttonOpenImage.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						//openOrShareImage(Intent.ACTION_VIEW, getString(R.string.str_title_open), uri);
						openOrShareImage(Intent.ACTION_VIEW, null, uri);
					}
				});

				// Create Button to Share the Image
				Button buttonShareImage = new Button(MainActivity.this);
				buttonShareImage.setText(R.string.str_btn_share_the_file);
				buttonShareImage.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openOrShareImage(Intent.ACTION_SEND, getString(R.string.str_title_share), uri);
						//openOrShareImage(Intent.ACTION_SEND, null, uri);
					}
				});

				// Add buttons to the root layout
				rootLayout.addView(buttonOpenImage);
				rootLayout.addView(buttonShareImage);

				// builder.setView(rootLayout);
				builder.setContentView(rootLayout);

				// Set up dialog buttons
				// builder.setNegativeButton(R.string.Close, null);

				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialogInterface) {
						dialogInterface.cancel();
					}
				});

				// AlertDialog dialog = builder.create();
				BottomSheetDialog dialog = builder;

				dialog.setOnKeyListener(new Dialog.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
							dialog.cancel();
							return true;
						}

						return false;
					}
				});

				// DialogInterface.OnClickListener OnNegativeClick = new DialogInterface.OnClickListener() {
				View.OnClickListener OnNegativeClick = new View.OnClickListener() {
					@Override
					// public void onClick(DialogInterface dialog, int which) {
					public void onClick(View view) {
						dialog.cancel();
					}
				};

				/*
				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialogInterface) {
						// Button buttonNegative = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
						buttonNegative.setOnClickListener(OnNegativeClick);
					}
				});
				*/

				dialog.setCancelable(true);

				dialog.setCanceledOnTouchOutside(true);

				dialog.show();

				WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

				if (dialog.getWindow() != null) {
					layoutParams.copyFrom(dialog.getWindow().getAttributes());
					layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
					layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
					layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
					layoutParams.x = 0; // Horizontal offset
					layoutParams.y = 0; // Vertical offset

					dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					dialog.getWindow().setAttributes(layoutParams);
					dialog.getWindow().setDimAmount(0.25f);
				}

			}
		});

	}

	private String getFileName(Context context, Uri uri) {
		String fileName = null;
		String scheme = uri.getScheme();

		// Check if the URI scheme is "file"
		if ("file".equals(scheme)) {
			fileName = uri.getLastPathSegment();  // Retrieve the last path segment as the file name
		}
		// Check if the URI scheme is "content"
		else if ("content".equals(scheme)) {
			// Try to get the display name using OpenableColumns
			Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
			if (returnCursor != null) {
				try {
					int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					if (nameIndex != -1 && returnCursor.moveToFirst()) {
						fileName = returnCursor.getString(nameIndex);
					}
				} finally {
					returnCursor.close();  // Always close the cursor to avoid memory leaks
				}
			}

			// If fileName is still null, try querying MediaStore
			if (fileName == null) {
				String[] projection = {MediaStore.Images.Media.TITLE};
				Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
				if (cursor != null) {
					try {
						if (cursor.moveToFirst()) {
							int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
							fileName = cursor.getString(columnIndex);
						}
					} finally {
						cursor.close();  // Close cursor
					}
				}
			}
		}

		// If fileName is still null, use the path as a fallback
		if (fileName == null) {
			fileName = uri.getPath();
			if (fileName != null) {
				int cut = fileName.lastIndexOf('/');
				if (cut != -1) {
					fileName = fileName.substring(cut + 1);
				}
			}
		}

		return fileName;
	}

	/*
	private static final int REQUEST_PERMISSION = 1;

	public static void requestPermission() {
		if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions((MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
		}
	}

	public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, Context context, String fileUrl) {
		if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			startDownload(context, fileUrl);
		} else {
			Toast toast = makeToast(context, "Permission denied. Unable to download file.", R.drawable.ic_icon_error, Toast.LENGTH_SHORT);
			toast.show();
		}
	}
	*/

	public void openOrShareImage(String action, String chooser, Uri uri) {
		String mime = "image/*";

		MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

		if (mimeTypeMap.hasExtension(mimeTypeMap.getFileExtensionFromUrl(uri.toString())))
			mime = mimeTypeMap.getMimeTypeFromExtension(mimeTypeMap.getFileExtensionFromUrl(uri.toString()));

		final Intent intent = new Intent(action, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setAction(action); // Intent.ACTION_VIEW, Intent.ACTION_SEND
		intent.setDataAndType(uri, mime);
		// intent.setFlags() or intent.addFlags()
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		//intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.putExtra(Intent.EXTRA_STREAM, uri);

		try {
			Context context = MainActivity.this;
			context.startActivity(chooser == null || chooser.isBlank() ? intent : Intent.createChooser(intent, chooser.trim()));
		}
		catch (Exception e) {
			e.printStackTrace();
			// Toast toast = makeToast(getApplicationContext(), e.getMessage(), R.drawable.ic_icon_error, Toast.LENGTH_LONG);
			// toast.show();
		}
	}

	public void openOrShareImage(String action, String chooser, File file) {
		// MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(imageId).build();
		// Uri.parse("file:///sdcard/file.jpg")

		Uri uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
				// this, BuildConfig.APPLICATION_ID + ".provider",
				FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", file) :
				Uri.fromFile(file);

		openOrShareImage(action, chooser, uri);
	}

	public void openOrShareImage(String action, String chooser, String path) {
		// context.getFilesDir(), Environment.getExternalStorageDirectory(), root.getAbsolutePath()
		// "file:///sdcard/file.jpg"

		File file = new File(path);
		openOrShareImage(action, chooser, file);
	}

	public ContentValues contentValues() {
		ContentValues values = new ContentValues();
		// values.put(MediaStore.Images.Media.TITLE, "Title Of Image");
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
		values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
		values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
		return values;
	}

	ImageView lastToastImage;
	TextView lastToastText;

	public Toast makeToast(Context context, Integer textId, Integer drawableId, int duration) {
		context = new ContextThemeWrapper(context, R.style.AppTheme); // getApplicationContext()

		final Toast toast = Toast.makeText(context, textId, duration);

		LayoutInflater inflater = getLayoutInflater();
		View toastLayout = inflater.inflate(R.layout.toast_layout, (ViewGroup) findViewById(R.id.toastLayout));

		ImageView imageView = (ImageView) toastLayout.findViewById(R.id.imageView);
		if (drawableId == null) imageView.setVisibility(View.GONE);
		else imageView.setImageResource(drawableId);
		lastToastImage = imageView;

		TextView textView = (TextView) toastLayout.findViewById(R.id.textView);
		textView.setText(textId);
		textView.setMinWidth(120);
		textView.setTextColor(getColor(R.color.colorForeground));
		textView.setGravity(Gravity.START);
		textView.setPadding(0, 0, 30, 0);
		lastToastText = textView;

		toast.setView(toastLayout);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);

		LinearLayout toastContent = toastLayout.findViewById(R.id.toastContent);

		if (SystemLanguage.equals("F") || SystemLanguage.equals("A")) {
			// toastContent.removeView(imageView);
			// toastContent.addView(imageView); // append first view to last.
			textView.setGravity(Gravity.END);
			textView.setPadding(30, 0, 0, 0);
		}

		return toast;
	}
	public Toast makeToast(Context context, String message, Integer drawableId, int duration) {
		context = new ContextThemeWrapper(context, R.style.AppTheme); // getApplicationContext()

		final Toast toast = Toast.makeText(context, message, duration);

		LayoutInflater inflater = getLayoutInflater();
		View toastLayout = inflater.inflate(R.layout.toast_layout, (ViewGroup) findViewById(R.id.toastLayout));

		ImageView imageView = (ImageView) toastLayout.findViewById(R.id.imageView);
		if (drawableId == null) imageView.setVisibility(View.GONE);
		else imageView.setImageResource(drawableId);
		lastToastImage = imageView;

		TextView textView = (TextView) toastLayout.findViewById(R.id.textView);
		textView.setText(message);
		textView.setMinWidth(120);
		textView.setTextColor(getColor(R.color.colorForeground));
		textView.setGravity(Gravity.START);
		textView.setPadding(0, 0, 30, 0);
		lastToastText = textView;

		toast.setView(toastLayout);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);

		LinearLayout toastContent = toastLayout.findViewById(R.id.toastContent);

		if (SystemLanguage.equals("F") || SystemLanguage.equals("A")) {
			// toastContent.removeView(imageView);
			// toastContent.addView(imageView); // append first view to last.
			textView.setGravity(Gravity.END);
			textView.setPadding(30, 0, 0, 0);
		}

		return toast;
	}

	public void setTextViewMessage(String string) {
		TextView textViewMessage = findViewById(R.id.textViewMessage);
		textViewMessage.setText(string);
		textViewMessage.setTextColor(getColor(R.color.colorPanelText));
		textViewMessage.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);
		textViewMessage.setAlpha(1f);
		applyViewDirection(textViewMessage);
		Log.i("setTextViewMessage", string);
	}
	public void setTextViewMessage(int stringId) {
		TextView textViewMessage = findViewById(R.id.textViewMessage);
		textViewMessage.setText(stringId);
		textViewMessage.setTextColor(getColor(R.color.colorPanelText));
		textViewMessage.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);
		textViewMessage.setAlpha(1f);
		applyViewDirection(textViewMessage);
		Log.i("setTextViewMessage", getString(stringId));
	}
	public void setTextViewMessage(Spannable spannable) {
		TextView textViewMessage = findViewById(R.id.textViewMessage);
		textViewMessage.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);
		textViewMessage.setText(spannable);
		textViewMessage.setTextColor(getColor(R.color.colorPanelText));
		textViewMessage.setAlpha(1f);
		applyViewDirection(textViewMessage);
		Log.i("setTextViewMessage", spannable.toString());
	}

	public void applyViewDirection(View view) {
		if (SystemLanguage.equals("F") || SystemLanguage.equals("A")) {
			view.setTextDirection(View.TEXT_DIRECTION_RTL);
		}
		else {
			view.setTextDirection(View.LAYOUT_DIRECTION_LTR);
		}
	}

	public View getDialogTitle(Dialog dialog, String title, int iconDrawable) {
		// Remove the existing title from the dialog
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Get the icon size and padding size
		int iconSize = getResources().getDimensionPixelSize(R.dimen.icon_size),
				paddingSize = getResources().getDimensionPixelSize(R.dimen.padding_size);

		// Create a LinearLayout
		LinearLayout titleLayout = new LinearLayout(this);
		titleLayout.setOrientation(LinearLayout.HORIZONTAL);
		titleLayout.setGravity(Gravity.CENTER_VERTICAL);
		titleLayout.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		));

		// Create an ImageView
		ImageView imageView = new ImageView(this);
		LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(iconSize, iconSize);
		imageViewParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		imageView.setLayoutParams(imageViewParams);
		imageView.setImageResource(iconDrawable);

		// Create a TextView
		TextView textView = new TextView(this);
		LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
				0,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				1.0f
		);
		textViewParams.gravity = Gravity.CENTER_VERTICAL;
		textView.setLayoutParams(textViewParams);
		textView.setTextAppearance(this, R.style.TextAppearanceMedium);
		textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		textView.setTextColor(getColor(R.color.colorForeground));
		textView.setText(title);
		textView.setPadding(paddingSize, 0,paddingSize, 0);

		// Create a Button (with transparent background)
		Button closeButton = new Button(this);
		LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(iconSize, iconSize);
		buttonParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
		closeButton.setLayoutParams(buttonParams);
		closeButton.setBackgroundResource(android.R.drawable.ic_menu_close_clear_cancel);
		closeButton.setOnClickListener(v -> dialog.dismiss());

		// Add the views to the LinearLayout
		titleLayout.addView(imageView);
		titleLayout.addView(textView);
		titleLayout.addView(closeButton);

		titleLayout.setPadding(0, 0, 0, paddingSize);

		return titleLayout;
	}

	// used in Persian apps
	private final String extendedArabic = "\u06f0\u06f1\u06f2\u06f3\u06f4\u06f5\u06f6\u06f7\u06f8\u06f9";

	// used in Arabic apps
	private final String arabic = "\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669";

	public String arabicToDecimal(String number) {
		char[] chars = new char[number.length()];
		for (int i = 0; i < number.length(); i++) {
			char ch = number.charAt(i);
			if (ch >= 0x0660 && ch <= 0x0669)
				ch -= 0x0660 - '0';
			else if (ch >= 0x06f0 && ch <= 0x06F9)
				ch -= 0x06f0 - '0';
			chars[i] = ch;
		}
		return new String(chars);
	}

	private Handler mHandler;
	private Runnable mRunnable;

	private String serverAddress;
	public SocketClient mSocket;

	boolean displayMessage = false;
	int timerCounter = 0;
	boolean iconConnectionBlink = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrintPaperFontSelect();
		ApplyAppLanguage();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (this.isFinishing())
			return;

		screenOn();
		setUiFlags();

		PackageManager p = getPackageManager();
		p.setComponentEnabledSetting(getComponentName(),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

		final Handler handler = new Handler(Looper.getMainLooper());

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {

				if (isPuRead || isPerformingRequest || isWaitingForTime)
				{
					secondPU++;

					if (secondPU >= 60) {
						secondPU = 0;
						minutePU++;
					}

					if (minutePU >= 60) {
						minutePU = 0;
						hourPU++;
						isPuRead = false;
					}

					if (hourPU >= 24) {
						hourPU = 0;
						dayPU++;
						isPuRead = false;
					}

					if (dayPU > 31) {
						dayPU = 1;
						monthPU++;
						isPuRead = false;
					}

					if (monthPU > 12) {
						monthPU = 1;
						yearPU++;
						isPuRead = false;
					}
				}

				if (System.currentTimeMillis() - lastWsUpdate >= 2000 && !isPerformingRequest && !isWaitingForTime) {

					if (mSocket != null && mSocket.isOpen()) {
						if (System.currentTimeMillis() - lastWsUpdate >= 8000) {
							if (isPuRead) checkAddressServer(true);
							if (isUpdating && mSocket != null) mSocket.close();
							isPuRead = false;
						}

						yearPU = 0;
						monthPU = 0;
						dayPU = 0;

						if ( !((TextView) findViewById(R.id.textViewMessage)).getText().toString().trim().equals(getString(R.string.str_rebooting)) )
						{
							TextView textViewWeight = findViewById(R.id.textViewWeight);
							textViewWeight.setText(R.string.Weight);
						}
					} else {
						isPuRead = false;

						if (!isWsConnecting && !isCheckingAddressServer && !isServerAvailable &&!isUpdating && !displayMessage && !checkAvailInBackground) {
							setTextViewMessage(R.string.Message);
						}
					}

					refreshConnectButton();

				}

				if (displayMessage && (timerCounter-- <= 0)) {
					displayMessage = false;
					timerCounter = 0;
				}

				if (!displayMessage) {

					if ((isPuRead || isPerformingRequest) && (isWaitingForTime || (yearPU > 0 && monthPU > 0 && dayPU > 0))) {
						if (!isWaitingForTime)
							displayDateTime();
					}

					else {
						isPuRead = false;

						if (isUpdating || ((TextView) findViewById(R.id.textViewMessage)).getText().toString().trim().equals(getString(R.string.str_rebooting)) )
							setTextViewMessage(R.string.str_rebooting);

						else if (isWsConnecting || (mSocket != null && mSocket.isOpen()))
							setTextViewMessage(R.string.str_connecting_to_device);

						else if (checkAvailInBackground && (System.currentTimeMillis() - lastWsUpdate < 2000))
							setTextViewMessage(R.string.str_warning_no_connection); // same as string in disconnect btn
					}

				}

				if (lastAvailCheck > 0 && System.currentTimeMillis() - lastAvailCheck >= 2000 && checkAvailInBackground && !isCheckingAddressServer && (mSocket == null || !mSocket.isOpen())) {
					lastAvailCheck = System.currentTimeMillis();
					isCheckingAddressServer = false;
					checkAddressServer(false);
				}

				TextView textViewMessage = (TextView) findViewById(R.id.textViewMessage);
				String currentText = textViewMessage.getText().toString().trim();

				if (currentText.equals(getString(R.string.str_warning_no_connection)) || currentText.equals(getString(R.string.str_connecting_to_device)))
				{
					iconConnectionBlink = !iconConnectionBlink;
				}
				else {
					iconConnectionBlink = false;
				}

				if (dialogSetTime != null) {

					try {
						TextView textView = (TextView) dialogSetTime.getWindow().findViewById(android.R.id.message);
						textView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);
						handleDialogSetTime(textView);
					}
					catch (Exception e) {
						e.printStackTrace();
					}

					dialogSetTime.show();
					mDialog = dialogSetTime;
				}

				refreshConnectButton();

				systemRoutine();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					if (handler.hasCallbacks(this)) {
						handler.removeCallbacks(this);
					}
				}

				handler.postDelayed(this,1000); // 1 second delay (takes millis)
			}
		};

		if (mSocket == null || !mSocket.isOpen())
			handler.post(runnable);

		boolean alreadyInitialized = mHandler != null && mRunnable != null;

		mHandler = handler;
		mRunnable = runnable;

		if (alreadyInitialized) return;


		final TextView textViewAppVersion = (TextView) findViewById(R.id.textViewAppVersion);
		String appName = getString(R.string.app_name);
		textViewAppVersion.setText(String.format("%s v%s", appName, BuildConfig.VERSION_NAME));
		textViewAppVersion.setTypeface(getTypeface(Typeface.DEFAULT, false), Typeface.BOLD);
		textViewAppVersion.setTextDirection(View.TEXT_DIRECTION_LTR);


		final CardView cardViewUserViewIconCard = (CardView) findViewById(R.id.imageViewUserIconCard);
		cardViewUserViewIconCard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				requestPassword();

			}
		});


		final TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);

		final ImageView imageViewUserIcon = (ImageView) findViewById(R.id.imageViewUserIcon);

		textViewUserName.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				cardViewUserViewIconCard.performClick();

			}
		});

		onUserNameUpdated();






		final TextView textViewWeight = findViewById(R.id.textViewWeight);
		textViewWeight.setTypeface(getTypeface(Typeface.MONOSPACE, true), Typeface.BOLD);
		textViewWeight.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
		textViewWeight.setTextDirection(View.TEXT_DIRECTION_LTR);

		final TextView textViewKg = findViewById(R.id.textViewKg);
		textViewKg.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
		textViewKg.setTextDirection(View.TEXT_DIRECTION_LTR);


		SharedPreferences settings = getSharedPreferences("ServerInfo", 0);

		final TextView textViewServerAddr = (TextView) findViewById(R.id.textViewServerAddr);
		textViewServerAddr.setTextDirection(View.TEXT_DIRECTION_LTR);

		if (serverAddress == null || serverAddress.isBlank())
			serverAddress = textViewServerAddr.getText().toString();

		if (settings.contains("Address"))
			serverAddress = settings.getString("Address", serverAddress);

		ImageView imageViewIconConnection = (ImageView) findViewById(R.id.imageViewIconConnection);

		imageViewIconConnection.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				textViewServerAddr.performClick();
			}
		});

		textViewServerAddr.setText(serverAddress);
		textViewServerAddr.setTypeface(getTypeface(Typeface.MONOSPACE, false), Typeface.BOLD);
		textViewServerAddr.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if (mSocket != null) {
					if (mSocket.isOpen()) {
						// Toast toast = makeToast(getApplicationContext(), R.string.str_before_edit_disconnect_first, null, Toast.LENGTH_SHORT);
						// toast.show();
						// return;

						// ? mSocket.close();
						isWsConnecting = false;
					} else {
						isPuRead = false;
						mSocket.close();

						if (isWsConnecting) {
							mSocket = null;
							isWsConnecting = false;
						}
					}
				} else {
					if (isWsConnecting)
						isWsConnecting = false;
				}

				checkAvailInBackground = false;

				runOnUiThread(new Runnable() {
					@Override
					public void run() {

						refreshConnectButton();

						AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);
						builder.setIcon(R.drawable.ic_icon_settings);
						builder.setTitle(R.string.str_device_address);

						// Set up the input
						final EditText input = new EditText(new ContextThemeWrapper(MainActivity.this, R.style.AppTheme_EditText));
						input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
						input.setImeOptions(input.getImeOptions() | EditorInfo.IME_FLAG_FORCE_ASCII);
						input.setImeActionLabel(getString(R.string.Confirm), EditorInfo.IME_ACTION_DONE);
						input.setSingleLine();
						input.setText(textViewServerAddr.getText());
						// input.setHint(R.string.str_enter_device_address);
						input.setTypeface(getTypeface(Typeface.MONOSPACE, false), Typeface.NORMAL);
						input.setTextSize(16);
						input.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
						input.setTextDirection(View.TEXT_DIRECTION_LTR);

						// Create a LinearLayout as the root view
						LinearLayout rootLayout = new LinearLayout(MainActivity.this);
						rootLayout.setOrientation(LinearLayout.VERTICAL);

						final int alertDialogPadding = getDimensionFromAttribute(MainActivity.this, R.attr.dialogPreferredPadding, 64);

						rootLayout.setPadding(alertDialogPadding, 64, alertDialogPadding, 64);

						// Create a TextView for prompt
						TextView textView = new TextView(MainActivity.this);
						textView.setText(R.string.str_enter_device_address);

						textView.setTextColor(getColor(R.color.colorForeground));
						textView.setTextSize(16);
						textView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);

						// Create a LinearLayout for hint
						LinearLayout hintView = new LinearLayout(MainActivity.this);
						hintView.setLayoutParams(new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT));
						hintView.setOrientation(LinearLayout.HORIZONTAL);
						hintView.setPadding(4, 4, 4, 0);

						// Create the bold TextView
						TextView hintCaption = new TextView(MainActivity.this);
						hintCaption.setText(R.string.str_required_format);
						hintCaption.setTextSize(14);
						hintCaption.setTypeface(getTypeface(Typeface.DEFAULT_BOLD, true), Typeface.BOLD);
						hintCaption.setTextColor(getColor(R.color.colorGrayText));
						hintCaption.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
						hintCaption.setGravity(Gravity.START);

						// Create the normal TextView
						TextView hintContent = new TextView(MainActivity.this);
						hintContent.setText(R.string.ip_port);
						hintContent.setTextSize(14);
						hintContent.setTextColor(getColor(R.color.colorGrayText));
						hintContent.setTypeface(getTypeface(Typeface.DEFAULT, false), Typeface.NORMAL);
						hintContent.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
						hintContent.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
						hintContent.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
						hintContent.setTextDirection(View.TEXT_DIRECTION_LTR);
						hintContent.setGravity(Gravity.END);

						// Add the TextViews to the LinearLayout for hint
						hintView.addView(hintCaption);
						hintView.addView(hintContent);

						// Add the views to the root LinearLayout
						rootLayout.addView(textView);
						rootLayout.addView(input);
						rootLayout.addView(hintView);

						builder.setView(rootLayout);

						// Set up the positive and negative buttons
						builder.setPositiveButton(R.string.Confirm, null);
						builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								refreshConnectButton();
							}
						});
						builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialogInterface) {
								checkAvailInBackground = true;

								InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
								inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
								inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);
								refreshConnectButton();
							}
						});

						builder.setOnKeyListener(new Dialog.OnKeyListener() {
							@Override
							public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
								if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
									// Perform click on the negative button
									Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
									button.performClick();
									return true;
								}

								if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
									// Find the currently focused view
									View focusedView = ((AlertDialog) dialog).getCurrentFocus();
									if (focusedView != null) {
										// Perform click on the focused view
										focusedView.performClick();
										return true; // Indicate that the key event has been handled
									}
								}

								return false;
							}
						});

						InputFilter[] filters = new InputFilter[1];

						filters[0] = new InputFilter() {
							@Override
							public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
								if (end > start) {
									String destTxt = dest.toString();
									String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);

									resultingTxt = arabicToDecimal(resultingTxt);

									if ((end - start) == 1 && (dend - dstart) == 0 && destTxt.length() - (dend + 1) > 0 && source.subSequence(start, end).equals(destTxt.substring(dstart, dend + 1))) {
										if (source.subSequence(start, end).toString().matches("[.:]"))
											input.setSelection(
													input.getSelectionStart() + (end - start),
													input.getSelectionEnd() + (end - start));
									}

									if (!resultingTxt.matches("^([[a-zA-Z0-9]\\-\\.]+)(:\\d*)?$") || resultingTxt.matches("^.*\\.{2,}.*$"))
										return "";

									return arabicToDecimal(source.subSequence(start, end).toString());
								}
								return null;
							}
						};

						input.setFilters(filters);

						input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
							@Override
							public void onFocusChange(View v, boolean hasFocus) {
								input.post(new Runnable() {
									@Override
									public void run() {
										InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
										inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
									}
								});
							}
						});

						input.requestFocus();

						String text = input.getText().toString();

						if (text.matches("^[0.:]*$"))
							input.setSelection(0, text.length());

						AlertDialog dialog = builder.create();

						// DialogInterface.OnClickListener OnPositiveClick = new DialogInterface.OnClickListener() {
						View.OnClickListener OnPositiveClick = new View.OnClickListener() {
							@Override
							// public void onClick(DialogInterface dialog, int which) {
							public void onClick(View view) {
								boolean dismiss = true;
								String str = input.getText().toString().trim();

								if (!str.isBlank()) {
									str = arabicToDecimal(str);

									str = str.replaceAll(":+$", "");

									if (str.equals(serverAddress) && (mSocket != null && mSocket.isOpen())) {
										refreshConnectButton();
									}

									else if (!str.matches("^([a-zA-Z0-9\\-\\.]+)(:\\d+)?$")
											|| str.matches("^.*[.:]{2,}.*$") || str.matches("^\\..*$")
											|| str.matches("^.*\\.$")) {
										Toast toast = makeToast(MainActivity.this, R.string.str_invalid_address_input, R.drawable.ic_icon_warning, Toast.LENGTH_LONG);
										toast.show();

										dismiss = false; // do not dismiss
									}

									else
										try {
											int port = 0;

											if (str.matches("^[0.:]+$")) {
												dismiss = false; // do not dismiss
											} else if (str.contains(":")) {
												String[] parts = str.split(":");

												try {
													port = Integer.parseInt(parts[1]);
												}
												catch (NumberFormatException e) {
													// ignored number parse error
												}

												if (!(port > 0 && port <= 65535)) {
													Toast toast = makeToast(MainActivity.this, R.string.str_invalid_port_input, R.drawable.ic_icon_warning, Toast.LENGTH_LONG);
													toast.show();

													dismiss = false; // do not dismiss
												}

												str = parts[0] + ":" + port;
											} else {

												/*
												final AlertDialog originalDialog = dialog;

												DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

													@Override
													public void onClick(DialogInterface dialog, int which) {
														final String text = input.getText().toString();

														switch (which) {
															case DialogInterface.BUTTON_POSITIVE: // Yes button clicked

																if (!text.contains(":")) {
																	input.append(":80");
																}

																Button button = ((AlertDialog) originalDialog).getButton(AlertDialog.BUTTON_POSITIVE);
																button.performClick();

																break;

															case DialogInterface.BUTTON_NEGATIVE: // No button clicked
																Toast toast = makeToast(MainActivity.this, R.string.str_enter_port_after_colon, R.drawable.ic_icon_information, Toast.LENGTH_LONG);
																toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 50);
																toast.show();

																if (!text.contains(":")) {
																	input.append(":");
																}

																break;
														}

														dialog.dismiss();
													}
												};

												AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);
												builder.setTitle(R.string.str_title_port_not_specified)
														.setMessage(R.string.str_question_use_default_port)
														.setPositiveButton(R.string.Yes, dialogClickListener)
														.setNegativeButton(R.string.No, dialogClickListener)
														.show();

												dismiss = false;
												*/

												final String text = input.getText().toString();
												if (!text.contains(":")) {
													input.append(":");
													input.setSelection(input.length());
												}

												String errorMessage = getString(R.string.str_enter_port_at_the_end).replaceAll(":", "") + ".";
												ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(getApplicationContext(), R.color.colorError));
												SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(errorMessage);
												spannableStringBuilder.setSpan(foregroundColorSpan, 0, errorMessage.length(), 0);
												Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_icon_error);
												drawable.setBounds(0, 0, input.getLineHeight(), input.getLineHeight());
												input.setError(spannableStringBuilder, drawable);

												// Toast toast = makeToast(MainActivity.this, R.string.str_enter_port_at_the_end, R.drawable.ic_icon_information, Toast.LENGTH_LONG);
												// toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 50);
												// toast.show();

												dismiss = false;

											}

											if (dismiss) // no errors
											{
												InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
												inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
												inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);

												URI addr = new URI(String.format("http://%s/", str));
												serverAddress = str;
												textViewServerAddr.setText(serverAddress);
												input.setText(serverAddress);

												SharedPreferences settings = getSharedPreferences("ServerInfo", 0);
												SharedPreferences.Editor editor = settings.edit();

												editor.putString("Address", textViewServerAddr.getText().toString());
												if (!editor.commit()) {
													Toast toast = makeToast(getApplicationContext(), R.string.str_device_address_not_saved, R.drawable.ic_icon_warning, Toast.LENGTH_SHORT);
													toast.show();
												}

												if (mSocket != null && mSocket.isOpen())
													mSocket.close();

												else {
													mSocket = null;
													isWsConnecting = false;
												}

												isPuRead = false;

												checkAvailInBackground = true;
												lastAvailCheck = 0;

												checkAddressServer(true);
												refreshConnectButton();

											}
										} catch (URISyntaxException e) {
											e.printStackTrace();

											Toast toast = makeToast(getApplicationContext(), e.getMessage(), R.drawable.ic_icon_error, Toast.LENGTH_LONG);
											toast.show();

											dismiss = false;
										}
								}

								input.requestFocus();

								if (dismiss) {
									dialog.dismiss();
									refreshConnectButton();
								}
							}
						};

						dialog.setOnShowListener(new DialogInterface.OnShowListener() {
							@Override
							public void onShow(DialogInterface dialogInterface) {
								Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
								button.setOnClickListener(OnPositiveClick);
							}
						});

						input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
							@Override
							public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

								boolean isEnter = false;

								// If triggered by an enter key, this is the event; otherwise, this is null.
								if (keyEvent != null) {
									// if shift key is down, then we want to insert the '\n' char in the TextView;
									// otherwise, the default action is to send the message.
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
										if (keyEvent.isShiftPressed() && !input.isSingleLine()) {
											return false;
										}
									}

									isEnter = (keyEvent.getAction() == KeyEvent.ACTION_DOWN
											&& keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER);
								}

								// Identifier of the action. This will be either the identifier you supplied,
								// or EditorInfo.IME_NULL if being called due to the enter key being pressed.
								if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
									Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
									button.performClick();

									input.requestFocus();

									// Return true if you have consumed the action, otherwise false.
									return true;
								}

								return false;
							}
						});

						input.setOnKeyListener(new View.OnKeyListener() {
							@Override
							public boolean onKey(View view, int keyCode, KeyEvent event) {
								if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
									Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
									button.performClick();

									input.requestFocus();

									return true;
								}
								return false;
							}
						});

						input.addTextChangedListener(new TextWatcher() {
							boolean deleting = false;
							int lastCount = 0;

							@Override
							public void beforeTextChanged(CharSequence s, int start, int count, int after) {
								// Nothing happens here

							}

							@Override
							public void onTextChanged(CharSequence s, int start, int before, int count) {
								deleting = lastCount >= count;
							}

							@Override
							public void afterTextChanged(Editable editable) {
								if (!deleting) {
									String text = editable.toString();

								}
							}
						});

						// builder.show();

						dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

						dialog.setCancelable(true);

						dialog.setCanceledOnTouchOutside(true);

						dialog.show();

						mDialog = dialog;

						refreshConnectButton();

						Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
							v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
						}

						InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
						// imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
						imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);

						input.postDelayed(new Runnable() {
							@Override
							public void run() {
								input.requestFocus();
								imm.showSoftInput(input, 0);
							}
						}, 500);

					}
				});
			}
		});

		onWsDisconnected(0, "", false);

		if (serverAddress == null || serverAddress.isBlank() || serverAddress.matches("^[0.:]+$") || serverAddress.matches("^.*:[:0]*$") || !settings.contains("Address"))
			textViewServerAddr.performClick();

		else {

			checkAddressServer(true);
			refreshConnectButton();

		}

		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);

		ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
			@Override
			public void onAvailable(Network network) {
				// network available
				checkAvailInBackground = true;
				checkAddressServer(false);
			}

			@Override
			public void onLost(Network network) {
				// network unavailable
			}
		};

		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			connectivityManager.registerDefaultNetworkCallback(networkCallback);
		} else {
			NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
			connectivityManager.registerNetworkCallback(request, networkCallback);
		}

		checkAvailInBackground = true;

		final ImageView imageViewPrintPaper = (ImageView) findViewById(R.id.imageViewPrintPaper);
		final int originalWidth = imageViewPrintPaper.getLayoutParams().width;
		final int originalHeight = imageViewPrintPaper.getLayoutParams().height;
		imageViewPrintPaper.setVisibility(View.GONE);

		imageViewPrintPaper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
				if (layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT || imageViewPrintPaper.getDrawable() == null) {
					layoutParams.width = originalWidth;
					layoutParams.height = originalHeight;
					imageViewPrintPaper.setBackgroundColor(Color.TRANSPARENT);
					imageViewPrintPaper.setVisibility(View.GONE);
				}
				/*
				else
				{
					layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
					layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
					imageViewPrintPaper.setBackgroundColor(getColor(R.color.colorDarkPaper));
					imageViewPrintPaper.setVisibility(View.VISIBLE);
				}
				*/
				view.setLayoutParams(layoutParams);
				view.requestLayout();
			}
		});

		Button buttonTare = (Button) findViewById(R.id.buttonTare);
		buttonTare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank() || !IsExecutable(R.id.buttonTare))
					return;

				if (isTaring) {
					Toast toast = makeToast(getApplicationContext(), R.string.str_another_request_already_performing, R.drawable.ic_icon_wait, Toast.LENGTH_SHORT);
					toast.show();
					return;
				}

				if (mSocket == null || !mSocket.isOpen())
					return;

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				String sUrl = String.format("http://%s/action/tare", serverAddress);
				new HttpRequest(MainActivity.this).execute(sUrl, "POST");
			}
		});

		Button buttonHide = (Button) findViewById(R.id.buttonHide);
		buttonHide.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank() || !IsExecutable(R.id.buttonHide))
					return;

				if (mSocket == null || !mSocket.isOpen())
					return;

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				String sUrl = String.format("http://%s/weight/display?value=hide", serverAddress);
				new HttpRequest(MainActivity.this).execute(sUrl, "POST");
			}
		});

		Button buttonShow = (Button) findViewById(R.id.buttonShow);
		buttonShow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank() || !IsExecutable(R.id.buttonShow))
					return;

				if (mSocket == null || !mSocket.isOpen())
					return;

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				String sUrl = String.format("http://%s/weight/display?value=show", serverAddress);
				new HttpRequest(MainActivity.this).execute(sUrl, "POST");
			}
		});

		Button buttonPower = (Button) findViewById(R.id.buttonPower);
		buttonPower.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank() || !IsExecutable(R.id.buttonPower))
					return;

				if (mSocket == null || !mSocket.isOpen())
					return;

				displayMessage = true;
				timerCounter = 2;

				setTextViewMessage(R.string.str_fetching_info);

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				String sUrl = String.format("http://%s/power/get", serverAddress);
				new HttpRequest(MainActivity.this).execute(sUrl, "GET");
			}
		});

		Button buttonReceipt = (Button) findViewById(R.id.buttonReceipt);
		buttonReceipt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank() || !IsExecutable(R.id.buttonReceipt))
					return;

				if (mSocket == null || !mSocket.isOpen())
					return;

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				if (lastReadStatus == 1) {
					displayResult(getString(R.string.str_operation_not_permitted));
					return;
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {

						if (PrintPaperInfo[0] > -1) {
							Toast toast = makeToast(getApplicationContext(), R.string.str_another_file_being_transmitted, R.drawable.ic_icon_wait, Toast.LENGTH_SHORT);
							toast.show();
							return;
						}

						PrintPaperInfo[0] = 1; // Receipt

						String sUrl = String.format("http://%s/is_exec", serverAddress);
						new HttpRequest(MainActivity.this).execute(sUrl, "GET");

					}
				});

			}
		});

		Button buttonReport = (Button) findViewById(R.id.buttonReport);
		buttonReport.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank() || !IsExecutable(R.id.buttonReport))
					return;

				if (mSocket == null || !mSocket.isOpen())
					return;

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				if (lastReadStatus == 1) {
					displayResult(getString(R.string.str_operation_not_permitted));
					return;
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {

						if (PrintPaperInfo[0] > -1) {
							Toast toast = makeToast(getApplicationContext(), R.string.str_another_file_being_transmitted, R.drawable.ic_icon_wait, Toast.LENGTH_SHORT);
							toast.show();
							return;
						}

						PrintPaperInfo[0] = 2; // Report

						String sUrl = String.format("http://%s/is_exec", serverAddress);
						new HttpRequest(MainActivity.this).execute(sUrl, "GET");

					}
				});

			}
		});

		Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
		buttonConnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				if (mSocket != null && mSocket.isOpen()) {
					TextView textViewMessage = findViewById(R.id.textViewMessage);
					if (textViewMessage.getText().toString().trim().equals(getString(R.string.str_warning_no_connection))) {
						checkAvailInBackground = false;
						if (mSocket != null)
							mSocket.close();
						isPuRead = false;
						refreshConnectButton();
						return;
					}

					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									// Yes button clicked
									checkAvailInBackground = false;
									if (mSocket != null)
										mSocket.close();
									isPuRead = false;
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									// No button clicked
									dialog.cancel();
									break;
							}

							dialog.dismiss();
							refreshConnectButton();
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);
					builder.setMessage(R.string.str_question_disconnect)
							.setTitle(R.string.btn_disconnect)
							.setIcon(R.drawable.ic_icon_disconnected)
							.setPositiveButton(R.string.Yes, dialogClickListener)
							.setNegativeButton(R.string.No, dialogClickListener)
							.show();

				} else if (isWsConnecting || (mSocket != null && mSocket.isClosing()) || (isCheckingAddressServer && !checkAvailInBackground)) {
					Toast toast = makeToast(MainActivity.this, R.string.str_wait_until_operation_finished, R.drawable.ic_icon_wait, Toast.LENGTH_SHORT);
					toast.show();
				} else if (serverAddress == null || serverAddress.isBlank() || serverAddress.matches("^[0.:]+$") || serverAddress.matches("^.*:[:0]*$")) {
					textViewServerAddr.performClick();
				} else {
					isPuRead = false;
					initWsConnection();
					// checkAvailInBackground = true;
				}

				refreshConnectButton();

			}
		});

		Button buttonSettings = (Button) findViewById(R.id.buttonSettings);
		buttonSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				TextView TextViewServerAddress = findViewById(R.id.textViewServerAddr);
				textViewServerAddr.performClick();

			}
		});

		buttonSettings.setText(R.string.SetIP);
		updateFunctionButtonState(buttonSettings, true);

		// Setup card click listeners to delegate to buttons
		setupCardClickDelegates();

		Button buttonDownload = (Button) findViewById(R.id.buttonDevelop);
		buttonDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				// initFileDownload();

				initProgressDialog();



			}
		});


		// TextView textViewWeight = (TextView) findViewById(R.id.textViewWeight);
		textViewWeight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (serverAddress == null || serverAddress.isBlank())
					return;

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

				/*
				if (mSocket != null && mSocket.isOpen())
				{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
					}

					mSocket.send("weight");
					return;
				}
				else
				{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
					}

					String sUrl = String.format("http://%s/weight/get", serverAddress);
					new HttpRequest(MainActivity.this).execute(sUrl, "GET");
					return;
				}
				*/

				// initWsConnection();
			}
		});

		TextView textViewMessage = findViewById(R.id.textViewMessage);
		textViewMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				if (isPuRead) displayDateTime();

				// yearPU = 0; monthPU = 0; dayPU = 0;
				// isPuRead = false;

				if (serverAddress != null && !serverAddress.isBlank() && !isPerformingRequest && !isWaitingForTime && !isUpdating && !isTaring) {
					isWaitingForTime = true;

					if (mSocket != null && mSocket.isOpen()) {
						mSocket.send("datetime");

						String sUrl = String.format("http://%s/pu", serverAddress);
						new HttpRequest(MainActivity.this).execute(sUrl, "GET");

						isPerformingRequest = true;
					}
					else {
						isPuRead = false;

						String sUrl = String.format("http://%s/datetime/get", serverAddress);
						new HttpRequest(MainActivity.this).execute(sUrl, "GET");

						isPerformingRequest = true;
					}

					textViewMessage.setAlpha(0.6f);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
					}
				}
			};
		});
		textViewMessage.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {

				if (textViewMessage.getText().toString().trim().equals(getString(R.string.str_warning_no_connection)))
					return false;

				if (serverAddress != null && !serverAddress.isBlank() && mSocket != null && mSocket.isOpen() && isPuRead) {

					runOnUiThread(new Runnable() {
						@Override
						public void run() {

							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle)
									.setIcon(R.drawable.ic_icon_clock)
									.setTitle(R.string.str_title_set_datetime)
									.setMessage(R.string.str_question_set_datetime)
									.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int whichButton)
										{
											dialog.dismiss();
											dialogSetTime = null;

											@SuppressLint("SimpleDateFormat")
											SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.ENGLISH);
											Date currentDate = new Date();
											String formattedDate = dateFormat.format(currentDate);

											String sUrl = String.format("http://%s/datetime/set?val=" + formattedDate, serverAddress);
											new HttpRequest(MainActivity.this).execute(sUrl, "POST");

											isPuRead = false;

											timerCounter = 2;
											displayMessage = true;

											setTextViewMessage(R.string.str_performing_request);
											refreshConnectButton();
										}

									})
									.setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {
											if (dialogSetTime != null) dialogSetTime.cancel();
										}
									})
									.setOnCancelListener(new DialogInterface.OnCancelListener() {
										@Override
										public void onCancel(DialogInterface dialogInterface) {
											dialogSetTime = null;
											refreshConnectButton();
										}
									})
									.setCancelable(true);

							AlertDialog dialog = builder.create();

							dialog.setOnShowListener(new DialogInterface.OnShowListener() {
								@Override
								public void onShow(DialogInterface dialogInterface) {
									Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
									// button.setOnClickListener(OnPositiveClick);

									try {
										TextView textView = (TextView) dialog.getWindow().findViewById(android.R.id.message);
										textView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);
										handleDialogSetTime(textView);
									}
									catch (Exception e) {
										e.printStackTrace();
									}
								}
							});

							dialogSetTime = dialog;

							dialog.show();

							mDialog = dialog;

							refreshConnectButton();

							try {
								TextView textView = (TextView) dialog.getWindow().findViewById(android.R.id.message);
								textView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}

					});

					// indicates that the long press event is consumed
					return true;

				}

				return false;

			}
		});

	}

	private void handleDialogSetTime(TextView textView) {

		final String FORCE_LTR = "\u200E\u202A\u202D";

		CharSequence chars = getString(R.string.str_question_set_datetime) + "\n\n" + FORCE_LTR + getLocalizedDeviceDate().replaceAll("[ \r\n\t]+", "  ");

		SpannableString s = new SpannableString(chars);

		int newlineIndex = chars.toString().indexOf("\n\n");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
			s.setSpan(new TypefaceSpan(getTypeface(Typeface.DEFAULT, true)), 0, newlineIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new TypefaceSpan(getTypeface(Typeface.MONOSPACE, true)), newlineIndex + 2, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new ForegroundColorSpan(getColor(isNightMode ? R.color.colorForeground : R.color.colorGrayText)), 0, newlineIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new ForegroundColorSpan(getColor(isNightMode ? R.color.colorAccent : R.color.colorForeground)), newlineIndex + 2, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new StyleSpan(Typeface.BOLD), newlineIndex + 2, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), newlineIndex + 2, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		dialogSetTime.setMessage(s);

	}

	private void setUiFlags() {
		Window window = getWindow();
		View decorView = window.getDecorView();
		// decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		// window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		// window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		// window.setStatusBarColor(Color.BLACK);
		// decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
	}

	private Typeface getTypeface(Typeface fontName, boolean localized) {
		// Typeface mFont = Typeface.createFromAsset(getAssets(), "fonts/myFont.ttf");
		// Typeface mFont = ResourcesCompat.getFont(getApplicationContext(), R.font.iransans);

		if (localized && (SystemLanguage.equals("F") || SystemLanguage.equals("A"))) {
			if (fontName == Typeface.DEFAULT || fontName == Typeface.DEFAULT_BOLD)
			{
				fontName = ResourcesCompat.getFont(getApplicationContext(), R.font.iransans);
			}
			else if (fontName == Typeface.MONOSPACE)
			{
				fontName = ResourcesCompat.getFont(getApplicationContext(), R.font.vazircode);
			}
		}
		else {
			if (fontName == Typeface.DEFAULT || fontName == Typeface.DEFAULT_BOLD)
			{
				// fontName = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
			}
			else if (fontName == Typeface.MONOSPACE)
			{
				fontName = ResourcesCompat.getFont(getApplicationContext(), R.font.monospace);
			}
		}

		if (fontName == null) fontName = Typeface.DEFAULT;

		return fontName;
	}

	private void screenOn() {
		Activity context = MainActivity.this;
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? pm.isInteractive() : pm.isScreenOn(); // check if screen is on
		if (!isScreenOn) {
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "thisApp:screenOn");
			wl.acquire(1000); //set your time in milliseconds
		}
		Window window = context.getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		if (BuildConfig.DEBUG) {
			// window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			setTurnScreenOn(true);
		}
	}

	private void initProgressDialog() {

		if (lastReadStatus == 1) {
			displayResult(getString(R.string.str_operation_not_permitted));
			return;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				// Environment.getExternalStorageDirectory().toString()
				String fileName = "img.jpg";
				File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

				// RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
				// setListeners(notificationLayout);

				// Create an instance of FileDownloader
				final FileDownloader downloadTask = new FileDownloader(MainActivity.this, String.format("http://%s/file/download?filename=fil/%s", serverAddress, fileName), outputFile);

				NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "download");
				notificationBuilder
						.setOngoing(true)
						.setAutoCancel(false)
						.setOnlyAlertOnce(true)
						.setContentTitle("File Download")
						.setContentText("Download in progress")
						.setTicker("Ticker Text")
						// .setPriority(Notification.PRIORITY_HIGH)
						// .setCategory(Notification.CATEGORY_PROGRESS)
						.setSmallIcon(R.drawable.ic_icon_save)
						.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo_pand))
						.setDefaults(DEFAULT_SOUND)
						.setDefaults(DEFAULT_VIBRATE)
						.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
						// .setContent(notificationView).build()
						// .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
						.setWhen(System.currentTimeMillis())
						.setShowWhen(false);
				notificationBuilder.setProgress(100, 0, true);

				Notification notification = notificationBuilder.build();
				notificationManager.notify(/*notificationID*/ 1, notification);
				notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);

				notificationBuilder.setSilent(true);
				notificationBuilder.setDefaults(Notification.DEFAULT_ALL);
				notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

				Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
				notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				notificationIntent.setAction(Intent.ACTION_MAIN);
				notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

				PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

				notificationBuilder.setContentIntent(pendingIntent);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				{
					String channelId = "download";
					NotificationChannel channel = new NotificationChannel(
							channelId,
							"Download File Information",
							NotificationManager.IMPORTANCE_LOW);
					channel.setDescription("Display information about file transfer.");
					notificationManager.createNotificationChannel(channel);
					notificationBuilder.setChannelId(channelId);
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);

				builder.setTitle("File Download");
				// builder.setMessage("Download in progress");
				builder.setIcon(R.drawable.ic_icon_save);
				builder.setCancelable(false);
				// progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

				final ProgressBar progressBar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleHorizontal);
				progressBar.setIndeterminate(true);

				// Create a LinearLayout as the root view
				LinearLayout rootLayout = new LinearLayout(MainActivity.this);
				rootLayout.setOrientation(LinearLayout.VERTICAL);

				int alertDialogPadding = getDimensionFromAttribute(MainActivity.this, R.attr.dialogPreferredPadding, 64);

				rootLayout.setPadding(alertDialogPadding, 32, alertDialogPadding, 32);

				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT,
						1);

				progressBar.setLayoutParams(layoutParams);

				// Set initial progress color
				progressBar.setIndeterminateTintList(ColorStateList.valueOf(getColor(R.color.colorSecondary)));

				LinearLayout textViewGroup = new LinearLayout(MainActivity.this);
				textViewGroup.setOrientation(LinearLayout.HORIZONTAL);
				textViewGroup.setLayoutParams(layoutParams);

				TextView percentageTextView = new TextView(MainActivity.this);
				TextView progressTextView = new TextView(MainActivity.this);

				percentageTextView.setText(String.format(Locale.getDefault(), "%d%%", 0));
				percentageTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
				percentageTextView.setTextSize(16); // 24
				percentageTextView.setTextColor(getColor(R.color.colorForeground));
				percentageTextView.setTypeface(getTypeface(Typeface.MONOSPACE, true), Typeface.BOLD);

				progressTextView.setText(R.string.str_loading);
				progressTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
				progressTextView.setTextSize(14);
				progressTextView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);

				LinearLayout.LayoutParams layoutParamsTextGroup = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.MATCH_PARENT,
						1);

				percentageTextView.setLayoutParams(layoutParamsTextGroup);
				progressTextView.setLayoutParams(layoutParamsTextGroup);

				textViewGroup.addView(progressTextView);
				textViewGroup.addView(percentageTextView);

				percentageTextView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
				progressTextView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

				rootLayout.addView(progressBar);
				rootLayout.addView(textViewGroup);

				builder.setView(rootLayout);

				final AlertDialog[] dialogQuestion = {null};

				builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) { dialogInterface.cancel(); }
				});
				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						downloadTask.cancel();
						dialog.dismiss();
						refreshConnectButton();
					}
				});
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (dialogQuestion[0] != null && dialogQuestion[0].isShowing()) dialogQuestion[0].dismiss();
						refreshConnectButton();
					}
				});

				builder.setOnKeyListener(new Dialog.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
							// Perform click on the negative button
							Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
							button.performClick();
							return true;
						}

						if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
							// Find the currently focused view
							View focusedView = ((AlertDialog) dialog).getCurrentFocus();
							if (focusedView != null) {
								// Perform click on the focused view
								focusedView.performClick();
								return true; // Indicate that the key event has been handled
							}
						}

						return false;
					}
				});

				final long[] mLastClickTime = {0, 0};

				final AlertDialog progressDialog = builder.create();

				// DialogInterface.OnClickListener OnNegativeClick = new DialogInterface.OnClickListener() {
				View.OnClickListener OnNegativeClick = new View.OnClickListener() {
					@Override
					// public void onClick(DialogInterface dialog, int which) {
					public void onClick(View view) {

						AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle)
								.setIcon(R.drawable.ic_icon_question)
								.setTitle(R.string.str_title_cancel_download)
								.setMessage(R.string.str_cancel_download)
								.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										progressDialog.cancel();
									}

								})
								.setNegativeButton(R.string.No, null);

						dialogQuestion[0] = builder.create();

						dialogQuestion[0].show();
					}
				};

				// progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

				// progressDialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_dialog));

				progressDialog.setCancelable(false);

				progressDialog.setCanceledOnTouchOutside(false);

				progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialogInterface) {
						Button buttonNegative = progressDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
						buttonNegative.setOnClickListener(OnNegativeClick);
					}
				});

				progressDialog.show();

				downloadTask.onFileInfoReceived = new FileDownloader.OnFileInfoReceived() {
					@Override
					public void onFileInfoReceived(FileDownloader downloadTask, long fileSize, String fileName) {
						progressBar.setIndeterminate(false);
						progressBar.setMax((int) fileSize);
						notificationBuilder.setProgress(100, 0, false).setContentInfo(0 + "%");
						notificationBuilder.setContentText(fileName).setOngoing(true);
						notificationManager.notify(/*NOTIFICATION_ID*/1, notificationBuilder.build());
					}
				};

				downloadTask.onProgressUpdate = new FileDownloader.OnProgressUpdate() {
					@Override
					public void onProgressUpdate(FileDownloader downloadTask, int progress) {
						progressBar.setProgress(progress);

						if (SystemClock.elapsedRealtime() - mLastClickTime[0] < 100) return;

						mLastClickTime[0] = SystemClock.elapsedRealtime();

						final int percentage = (int) (((float) progress / progressBar.getMax()) * 100);

						notificationBuilder.setOngoing(true)
								.setProgress(100, percentage, false)
								.setContentInfo(percentage + "%")
								.setSubText(percentage + "%");

						if (percentage != mLastClickTime[1])
						{
							mLastClickTime[1] = percentage;
							notificationManager.notify(/*NOTIFICATION_ID*/1, notificationBuilder.build());
						}

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try {
									final Locale locale = Locale.getDefault();
									setProgressCaption(progressTextView, progressBar);
									// progressTextView.setText(String.format(locale, "%d/%d", progressBar.getProgress(), progressBar.getMax()));
									percentageTextView.setText(String.format(locale, "%d%%", percentage));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				};

				downloadTask.onCompleted = new FileDownloader.OnCompleted() {
					@Override
					public void onCompleted(FileDownloader downloadTask) {
						progressDialog.dismiss();

						// cancel notification
						notificationManager.cancel(/*NOTIFICATION_ID*/1);

						if (downloadTask.isCancelled())
						{
							onFileReceiveStop();

							notificationBuilder.setContentText("Download cancelled").setSubText(null)
									.setOngoing(false)
									// Removes the progress bar
									.setProgress(0,0,false);
							notificationManager.notify(/*NOTIFICATION_ID*/1, notificationBuilder.build());

							Toast toast = makeToast(MainActivity.this, "Download cancelled", R.drawable.ic_icon_warning, Toast.LENGTH_SHORT);
							toast.show();
						}
					}

					@Override
					public void onCompleted(FileDownloader downloadTask, File outputFile) {
						lastReadStatus = -1;

						notificationBuilder.setContentText("Download completed").setSubText(null)
								.setOngoing(false)
								// Removes the progress bar
								.setProgress(0,0,false);
						notificationManager.notify(/*id*/ 1, notificationBuilder.build());

						Toast toast = makeToast(MainActivity.this, "Download completed", R.drawable.ic_icon_information, Toast.LENGTH_SHORT);
						toast.show();
					}
				};

				// Start the download
				downloadTask.execute("parameters passed here");

				/*
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							while (progressDialog.isShowing() && progressBar.getProgress() < progressBar.getMax()) {
								Thread.sleep(100);
								// handle.sendMessage(handle.obtainMessage());
								progressBar.incrementProgressBy(2);
								int percentage = (int) (((float) progressBar.getProgress() / progressBar.getMax()) * 100);
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										try {
											Locale locale = Locale.getDefault();
											progressTextView.setText(String.format(locale, "%d/%d", progressBar.getProgress(), progressBar.getMax()));
											percentageTextView.setText(String.format(locale, "%d%%", percentage));
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
							}
							if (progressDialog.isShowing())
							{
								progressDialog.dismiss();
							}
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
				*/

			}
		});
	}

	private void setProgressCaption(TextView progressTextView, ProgressBar progressBar) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			progressTextView.setText(String.format(Locale.getDefault(), "%s %s %s", humanBytes(progressBar.getProgress()), getString(R.string.str_of), humanBytes(progressBar.getMax())));
			return;
		}

		SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
		int start, end;

		String[] current = humanBytes(progressBar.getProgress()).split(" ", 2),
				total = humanBytes(progressBar.getMax()).split(" ", 2);

		spannableStringBuilder.append(current[0]);
		spannableStringBuilder.setSpan(
				new TypefaceSpan(getTypeface(Typeface.MONOSPACE, true)),
				start = 0,
				end = spannableStringBuilder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		);
		spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannableStringBuilder.setSpan(new ForegroundColorSpan(getColor(R.color.colorForeground)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		spannableStringBuilder
				.append(" ")
				.append(current[1])
				.append(" ")
				.append(getString(R.string.str_of))
				.append(" ");

		start = spannableStringBuilder.length();
		spannableStringBuilder.append(total[0]);
		spannableStringBuilder.setSpan(
				new TypefaceSpan(getTypeface(Typeface.MONOSPACE, true)),
				start,
				end = spannableStringBuilder.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		);
		spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spannableStringBuilder.setSpan(new ForegroundColorSpan(getColor(R.color.colorForeground)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		spannableStringBuilder
				.append(" ")
				.append(total[1]);

		progressTextView.setText(spannableStringBuilder);
	}

	public String humanBytes(long bytes)
	{
		int decimalPlaces = 0;

		if (bytes >= Math.pow(1024, 2))
			decimalPlaces = 2;

		return humanBytes(bytes, decimalPlaces);
	}

	public String humanBytes(long bytes, int decimalPlaces) {
		final String[] units = getResources().getStringArray(R.array.byte_units);
		double value = bytes;
		int unitIndex = 0;
		while (value >= 1024 && unitIndex < units.length - 1) {
			value /= 1024;
			unitIndex++;
		}
		return String.format(Locale.getDefault(), "%." + decimalPlaces + "f %s", value, units[unitIndex]);
	}

	private void onFileReceiveStop() {
		isPuRead = false;
		isPerformingRequest = false;
		lastReadStatus = -1;
		lastWsUpdate = System.currentTimeMillis();
	}

	public static int getDimensionFromAttribute(Context context, int attr, int defaultValue) {
		TypedValue typedValue = new TypedValue();
		if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
			return TypedValue.complexToDimensionPixelSize(typedValue.data, context.getResources().getDisplayMetrics());
		} else {
			return defaultValue;
		}
	}

	public String getLocalizedDeviceDate() {
		if (SystemLanguage.equals("F")) {
			int[] convertDate = new int[3];

			Date currentDate = new Date();
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(currentDate);

			int year   = calendar.get(Calendar.YEAR);
			int month  = calendar.get(Calendar.MONTH) + 1; // {0 - 11}
			int day    = calendar.get(Calendar.DAY_OF_MONTH);

			convertToShamsi(year, month, day, convertDate);

			year  = convertDate[0];
			month = convertDate[1];
			day   = convertDate[2];

			int hour   = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(Calendar.MINUTE);
			int second = calendar.get(Calendar.SECOND);

			return String.format("%4d/%02d/%02d %02d:%02d:%02d", year, month, day, hour, minute, second);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/M/d HH:mm:ss");
		Date currentDate = new Date();
		String formattedDate = dateFormat.format(currentDate);
		return formattedDate;
	}

	private boolean warnedIncorrectTime = false;

	private void displayDateTime() {
		if (yearPU <= 0 || monthPU <= 0 || dayPU <= 0) {
			isPuRead = false;
			if ((mSocket != null && mSocket.isOpen()) || isWsConnecting) return;
			setTextViewMessage(R.string.Message);
			return;
		}

		boolean incorrectPU = false;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			LocalDateTime dateTimePU = LocalDateTime.of(yearPU, monthPU, dayPU, hourPU, minutePU, secondPU);

			if (Math.abs(ChronoUnit.SECONDS.between(dateTimePU, LocalDateTime.now())) >= 120) {
				if (DeviceClockDiff >= 0 && DeviceClockDiff <= 60) {
					if (!warnedIncorrectTime) {
						warnedIncorrectTime = true;

						displayMessage = true;
						timerCounter = 2;

						setTextViewMessage("⚠️ " + getString(R.string.str_incorrect_datetime));
						TextView textViewMessage = findViewById(R.id.textViewMessage);
						textViewMessage.setTextColor(getColor(R.color.colorError));

						return;
					}
					incorrectPU = true;
				}
			}
		}

		displayMessage = false;
		timerCounter = 0;

		int year = yearPU, month = monthPU, day = dayPU;

		if (SystemLanguage.equals("F")) {
			int[] convertDate = new int[3];

			convertToShamsi(yearPU, monthPU, dayPU, convertDate);

			year = convertDate[0];
			month = convertDate[1];
			day = convertDate[2];
		}

		TextView textViewMessage = findViewById(R.id.textViewMessage);
		textViewMessage.setTextColor(getColor(incorrectPU ? R.color.colorError : R.color.colorPanelText));
		textViewMessage.setTypeface(getTypeface(Typeface.MONOSPACE, true), Typeface.BOLD);
		textViewMessage.setTextDirection(View.TEXT_DIRECTION_LTR);
		textViewMessage.setAlpha(1f);

		//  String currentText = textViewMessage.getText().toString().trim();

		textViewMessage.setText(String.format(getString(R.string.datetime_format), year, month, day, hourPU, minutePU, secondPU).replaceAll("[ \r\n\t]+", "  "));
	}

	public void refreshConnectButton() {
		ImageView imageViewIconConnection = (ImageView) findViewById(R.id.imageViewIconConnection);

		Button connectButton = findViewById(R.id.buttonConnect);
		if (mSocket != null && mSocket.isOpen()) {
			updateFunctionButtonState(connectButton, true);
			connectButton.setText(R.string.btn_disconnect);
			imageViewIconConnection.setImageResource(R.drawable.ic_icon_wifi);
		} else if (isWsConnecting || (mSocket != null && mSocket.isClosing()) || (isCheckingAddressServer && !checkAvailInBackground)) {
			updateFunctionButtonState(connectButton, false);
			connectButton.setText(R.string.Wait);
			imageViewIconConnection.setImageResource(R.drawable.ic_icon_wait);
		} else if (serverAddress == null || serverAddress.isBlank() || serverAddress.startsWith("0.0.0.0") || !isServerAvailable) {
			updateFunctionButtonState(connectButton, false);
			connectButton.setText(R.string.btn_connection);
			imageViewIconConnection.setImageResource(R.drawable.ic_icon_wait);
		} else {
			updateFunctionButtonState(connectButton, true);
			connectButton.setText(R.string.btn_connect);
			imageViewIconConnection.setImageResource(R.drawable.ic_icon_disconnected);

			TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);
			textViewUserName.setText("");
			onUserNameUpdated();
		}

		if (isWsConnecting || isPerformingRequest || isWaitingForTime)
			imageViewIconConnection.setImageResource(R.drawable.ic_icon_wait);

		if (mSocket == null || !mSocket.isOpen())
		{
			imageViewIconConnection.setImageResource(R.drawable.ic_icon_disconnected);

			TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);
			textViewUserName.setText("");
			onUserNameUpdated();
		}

		boolean isConnected = (mSocket != null && mSocket.isOpen());

		TextView textViewMessage = findViewById(R.id.textViewMessage);
		String currentText = textViewMessage.getText().toString().trim();
		if (currentText.equals(getString(R.string.str_connecting_to_device)) || currentText.equals(getString(R.string.str_warning_no_connection)))
		{
			// imageViewIconConnection.setImageResource(R.drawable.ic_icon_wait);
			imageViewIconConnection.setVisibility(iconConnectionBlink ? View.INVISIBLE : View.VISIBLE);

			if (!isPuRead && !isWsConnecting)
			{
				TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);
				textViewUserName.setText("");
				onUserNameUpdated();
			}
		}
		else
		{
			iconConnectionBlink = false;
			imageViewIconConnection.setVisibility(View.VISIBLE);
		}

		final TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);

		String textContent = textViewUserName.getText().toString();

		boolean canPerformAction = isConnected && isPuRead && !isPerformingRequest && !isTaring && accessLevel != CommonDefine.NotAccessLevel_ && accessLevel != CommonDefine.UnknownLevel_ && accessLevel < 255 && !textContent.isBlank() && hasWindowFocus() && (mDialog == null || !mDialog.isShowing());

		updateFunctionButtonState(findViewById(R.id.buttonShow), canPerformAction && IsExecutable(R.id.buttonShow) && ShowHiddenWeight != null && ShowHiddenWeight == false);
		updateFunctionButtonState(findViewById(R.id.buttonHide), canPerformAction && IsExecutable(R.id.buttonHide) && ShowHiddenWeight != null && ShowHiddenWeight == true);
		updateFunctionButtonState(findViewById(R.id.buttonPower), canPerformAction && IsExecutable(R.id.buttonPower));
		updateFunctionButtonState(findViewById(R.id.buttonTare), canPerformAction && IsExecutable(R.id.buttonTare));
		updateFunctionButtonState(findViewById(R.id.buttonReceipt), canPerformAction && IsExecutable(R.id.buttonReceipt));
		updateFunctionButtonState(findViewById(R.id.buttonReport), canPerformAction && IsExecutable(R.id.buttonReport));
	}

	private void updateFunctionButtonState(Button button, boolean state) {
		TypedValue typedValue = new TypedValue();
		getTheme().resolveAttribute(android.R.attr.disabledAlpha, typedValue, true);
		float disabledAlpha = typedValue.getFloat();

		if (!state) {
			button.setAlpha(disabledAlpha);
			button.setTextColor(getColor(R.color.colorGrayText));
		} else {
			button.setAlpha(1.0f);
			button.setTextColor(getColor(R.color.colorForeground));
		}
	}

	private boolean IsExecutable(int id)
	{
		if (IsExecutable_PU == -1 || accessLevel == CommonDefine.NotAccessLevel_ || accessLevel == CommonDefine.UnknownLevel_ || accessLevel >= 255) return false;

		switch (id)
		{
			case R.id.buttonShow:
			case R.id.buttonHide:
				return IsExecutable_PU != 0;

			case R.id.buttonPower:
				return true;

			case R.id.buttonReceipt:
			case R.id.buttonReport:

			case R.id.buttonTare:
				return IsExecutable_PU != 0;
		}

		// MaxWiFiAccess || ID_Index_PU == CommonDefine.WiFiRemoteUserPageIndex_ || ID_Index_PU == CommonDefine.RemoteManagerLevel_ || ID_Index_PU == CommonDefine.WiFiRemotePandPageIndex_

		return false;
	}

	/*
	private static final HashMap<Integer, ValueAnimator> animatorMap = new HashMap<>();

	public static void animateAlpha(View view, float targetAlpha) {
		int viewId = view.getId();
		ValueAnimator animator = animatorMap.get(viewId);

		// If an animation is already running, stop it and use the last value as the current position
		if (animator != null && animator.isRunning()) {
			animator.cancel();
			view.setAlpha((float) animator.getAnimatedValue());
		}

		// Create a new animator or reuse the existing one
		if (animator == null) {
			animator = ValueAnimator.ofFloat(view.getAlpha(), targetAlpha);
			animator.setDuration(500);
			animator.setInterpolator(new AccelerateDecelerateInterpolator());
			animator.addUpdateListener(animation -> view.setAlpha((float) animation.getAnimatedValue()));
			animatorMap.put(viewId, animator);
		} else {
			animator.setFloatValues(view.getAlpha(), targetAlpha);
		}

		// Start the animation
		animator.start();
	}
	*/

	/*
	public boolean isMobileDataEnabled() {
		boolean mobileDataEnabled = false;
		ConnectivityManager cm = (ConnectivityManager)
				getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			Class cmClass = Class.forName(cm.getClass().getName());
			Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
			method.setAccessible(true);

			mobileDataEnabled = (Boolean) method.invoke(cm);
		} catch (Exception e) {
			e.printStackTrace();

			String message = e.getMessage();

			if (message != null && !message.isBlank()) {
				Toast toast = makeToast(MainActivity.this, message, R.drawable.ic_icon_error, Toast.LENGTH_LONG);
				toast.show();
			}
		}
		return mobileDataEnabled;
	}
	*/

	private boolean isCheckingAddressServer = false, isServerAvailable = false, checkAvailInBackground = false;

	public void checkAddressServer(boolean showMessages) {

		if (isWsConnecting || isPerformingRequest || isCheckingAddressServer || (mSocket != null && mSocket.isOpen() && isPuRead))
			return;

		isCheckingAddressServer = true;

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {

				int timeout = 2000;

				try {
					// String ip = Pattern.compile(":.*$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(serverAddress).replaceAll("");

					// final boolean isReachable = InetAddress.getByName(ip).isReachable(timeout);

					String[] parts = Arrays.copyOf(serverAddress.split(":"), 2);

					if (parts[0] == null || parts[0].isBlank()) {
						isCheckingAddressServer = false;
						return;
					}

					if (parts[1] == null || parts[1].isBlank()) {
						parts[1] = "80";
					}

					int port = Integer.parseInt(parts[1]);

					if (port <= 0 || port >= 65535) {
						isCheckingAddressServer = false;
						return;
					}

					InetAddress ip = InetAddress.getByName(parts[0]);

					Socket socket = new Socket();

					try {
						socket.connect(new InetSocketAddress(ip, port), timeout);
					}

					catch (IOException e) {
						e.printStackTrace();
						socket.close();
					}

					final boolean wasConnected = socket.isConnected();

					socket.close();

					isPuRead = false;

					isCheckingAddressServer = false;

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (wasConnected) {
								if (!isPuRead && !isPerformingRequest && (mSocket == null || !mSocket.isClosing())) {
									checkAvailInBackground = false;
									String sUrl = String.format("http://%s/pu", serverAddress);
									new HttpRequest(MainActivity.this).execute(sUrl, "GET");
									isPerformingRequest = true;
								}
							} else {
								if (showMessages && !isUpdating && (mSocket == null || !mSocket.isOpen())) {
									Toast toast = makeToast(getApplicationContext(), R.string.str_device_not_reachable, R.drawable.ic_icon_disconnected, Toast.LENGTH_SHORT);
									toast.show();
								}
							}
							lastAvailCheck = System.currentTimeMillis();
							isCheckingAddressServer = false;
							isServerAvailable = wasConnected;
							refreshConnectButton();
						}
					});

					if (!wasConnected && mSocket != null && mSocket.isOpen()) {
						// mSocket.closeConnection(0, "");
					}

				} catch (final Exception e) {
					// Parse error here
					e.printStackTrace();
					isCheckingAddressServer = false;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							String message = e.getMessage();
							if (message != null && !(message = message.trim()).isBlank()) {
								// Toast toast = makeToast(getApplicationContext(), message, R.drawable.ic_icon_error, Toast.LENGTH_LONG);
								// toast.show();
							}
							isCheckingAddressServer = false;
						}
					});
				}

			}
		});

		thread.start();

	}

	public boolean isPerformingRequest = false;

	public void updateResults(String apiUrl, String result) {

		refreshConnectButton();

		if (apiUrl.contains("/pu")) {
			final TextView textViewSerialNumber = findViewById(R.id.textViewSerialNumber);

			if (result == null || result.isBlank()) {
				textViewSerialNumber.setText(R.string.str_connection_error);
				textViewSerialNumber.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);

				isPuRead = false;
				return;
			}

			final String[] parts = result.split("\n+");

			String currentUser = ((TextView) findViewById(R.id.textViewUserName)).getText().toString();

			if (currentUser.equals("--")) currentUser = "";

			if (parts[0].equalsIgnoreCase("Ready")) lastReadStatus = 0;
			if (parts[0].equalsIgnoreCase("Busy")) { lastReadStatus = 1; isPerformingRequest = true; }
			if (parts[0].equalsIgnoreCase("Booting")) lastReadStatus = 2;

			textViewSerialNumber.setText((Objects.equals(parts[0], "Ready") || !currentUser.isBlank()) && parts[1] != null && !parts[1].isBlank() ? String.format("%s %s", getString(R.string.SN).trim(), parts[1]) : getString(R.string.str_device_not_ready)); // Ready, Busy, Booting, Unknown
			textViewSerialNumber.setTypeface(
					textViewSerialNumber.getText().toString().startsWith(getString(R.string.SN).trim())
							? getTypeface(Typeface.MONOSPACE, false)
							: getTypeface(Typeface.DEFAULT, true)
					, Typeface.BOLD);

			if (!parts[0].isBlank() && (parts[0].equalsIgnoreCase("Ready") || parts[0].equalsIgnoreCase("Booting"))) {
				if (serverAddress == null || serverAddress.isBlank())
					return;

				isWaitingForTime = true;

				if (mSocket != null && mSocket.isOpen()) {
					mSocket.send("datetime");
				}
				else {
					new HttpRequest(MainActivity.this).execute(String.format("http://%s/datetime/get", serverAddress));
				}

				if (!isWsConnecting && (mSocket == null || !mSocket.isOpen()) && !isPuRead) {
					isPuRead = true;
					initWsConnection();
				}

				isPuRead = parts[0].equalsIgnoreCase("Ready") || parts[0].equalsIgnoreCase("Booting");
			} else {
				isPuRead = false;
			}

			if (!parts[0].isBlank()) {
				checkAvailInBackground = true;
			}

			refreshConnectButton();

			return;
		}

		if (apiUrl.contains("/page_id")) {
			result = result == null || result.isBlank() ? "" : result.trim();

			try {
				ID_Index_PU = !(result == null || result.isBlank() || result.equals("fail")) ? Integer.parseInt(result) : -1;
			} catch (Exception e) {
				ID_Index_PU = -1;
				e.printStackTrace();
			}

			refreshConnectButton();

			result = "";
		}

		if (apiUrl.contains("/is_exec")) {
			result = result == null || result.isBlank() ? "" : result.trim();

			try {
				IsExecutable_PU = !(result == null || result.isBlank() || result.equals("fail")) ? Integer.parseInt(result) : -1;
			} catch (Exception e) {
				IsExecutable_PU = -1;
				e.printStackTrace();
			}

			refreshConnectButton();

			result = "";

			if (PrintPaperInfo[0] > 0) {
				int id = 0;

				switch (PrintPaperInfo[0]) {
					case 1:
						id = R.id.buttonReceipt;
						break;
					case 2:
						id = R.id.buttonReport;
						break;
				}

				if (!IsExecutable(id)) {
					result = getString(R.string.str_operation_not_permitted);
					PrintPaperInfo[0] = -1;
				}

				else switch (PrintPaperInfo[0]) {
					case 1:
						getReceipt();
						break;
					case 2:
						getReport();
						break;
				}
			}
		}

		if (apiUrl.contains("/weight/display")) {
			result = result == null || result.isBlank() ? "" : result.trim();

			if (result != null && !result.isBlank()) {
				if (result.equalsIgnoreCase("shown")) ShowHiddenWeight = true;
				if (result.equalsIgnoreCase("hidden")) ShowHiddenWeight = false;
			}

			refreshConnectButton();

			return;
		}

		if (apiUrl.contains("/username")) {
			final TextView textViewUserName = findViewById(R.id.textViewUserName);

			if (result == null || result.isBlank())
				result = "";

			final String[] parts = result.split("\n+");

			try {
				final String currentUser = stringManagement(URLDecoder.decode(parts[0] == null || parts[0].isBlank() ? "" : parts[0].trim(), "ISO-8859-1")).trim();
				textViewUserName.setText(!currentUser.equals("--") ? currentUser : "");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				textViewUserName.setText("");
			}

			try {
				accessLevel = !(parts[1] == null || parts[1].isBlank()) ? Integer.parseInt(parts[1].trim()) : 0;
			} catch (Exception e) {
				e.printStackTrace();
				accessLevel = 0;
			}

			try {
				IsExecutable_PU = !(parts[2] == null || parts[2].isBlank()) ? Integer.parseInt(parts[1].trim()) : 0;
			} catch (Exception e) {
				e.printStackTrace();
				IsExecutable_PU = 0;
			}

			onUserNameUpdated();

			refreshConnectButton();

			return;
		}

		if (apiUrl.contains("/lang")) {
			result = result == null || result.isBlank() ? "" : result.trim();

			if (result != null && !result.isBlank()) {
				if (result.length() == 1)
				{
					isLanguageReceivedFromPu = true;
					SystemLanguage = result;
					UpdateAppLanguage();
				}
				else {
					Toast toast = makeToast(getApplicationContext(), R.string.str_language_format_invalid, R.drawable.ic_icon_caution, Toast.LENGTH_LONG);
					toast.show();
				}
				return;
			}

			return;
		}

		String Kind = getString(R.string.File);

		if (apiUrl.contains("/receipt")) {
			Kind = getString(R.string.Receipt);
			PrintPaperInfo[0] = 1;
		}

		if (apiUrl.contains("/report")) {
			Kind = getString(R.string.Report);
			PrintPaperInfo[0] = 2;
		}

		if (apiUrl.contains("/receipt") || apiUrl.contains("/report")) {
			result = result == null || result.isBlank() ? "" : result.trim();

			if (!result.contains("result:") && !result.isBlank()) {
				// BulkData = result.getBytes();
				drawPrintPaper();
				result = String.format(getString(R.string.str_item_received), Kind);
				// return;
			} else {
				PrintPaperInfo[0] = -1;
			}
			if (result.equals("result: " + CommonDefine.result_Not_Found))
				result = String.format(getString(R.string.str_specified_not_exist), Kind);
			if (result.equals("result: " + CommonDefine.result_NotAcceptable))
				result = getString(R.string.str_operation_not_permitted);
		}

		if (apiUrl.contains("/weight/get")) {
			final TextView textViewWeight = findViewById(R.id.textViewWeight);

			result = result == null || result.isBlank() ? "" : result.trim();

			if (result.isBlank())
				result = getString(R.string.Weight);

			if (result.equalsIgnoreCase("off"))
				result = "PAND";

			textViewWeight.setText(result);
			return;
		}

		if (apiUrl.contains("/power/get")) {
			result = result == null || result.isBlank() ? "" : result.trim();

			String[] parts = result.split(",");

			if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
				try {
					result = String.format("%s%s%s",
							Integer.parseInt(parts[0]) == 1 ? getString(R.string.str_grid_power) : getString(R.string.str_backup_battery), getString(R.string.Comma),
							String.format(getString(R.string.str_battery_voltage_formatted), (float) (Integer.parseInt(parts[1])) / 100.0));

					displayMessage = true;
					timerCounter = 3;

					setTextViewMessage(result);
				}
				catch (Exception e) {
					Toast toast = makeToast(getApplicationContext(), getString(R.string.str_request_failed), R.drawable.ic_icon_error, Toast.LENGTH_LONG);
					toast.show();
					e.printStackTrace();
				}

				return;
			}
		}

		if (apiUrl.contains("tare") && result != null && result.trim().equals("fail"))
			result = getString(R.string.str_operation_not_permitted);

		if (apiUrl.contains("tare") && result != null && result.trim().equals("ok!") ) {
			result = getString(R.string.str_operation_received);

			final String currentText = ((TextView) findViewById(R.id.textViewMessage)).getText().toString();

			if (currentText.equals(getString(R.string.str_performing_request))) {
				TextView textViewWeight = findViewById(R.id.textViewWeight);
				textViewWeight.setText("⌛"); // getString(R.string.Tare)

				TextView textViewMessage = findViewById(R.id.textViewMessage);
				textViewMessage.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);

				return;
			}

		}

		if (result != null && result.trim().equals("fail"))
			result = getString(R.string.str_request_failed).replaceAll(":", "") + ".";

		if (result != null && result.trim().equals("ok!"))
			result = getString(R.string.Done);

		if (result != null && !result.isBlank()) {
			Pattern pattern = Pattern.compile("([0-9]{4})/([0-9]+)/([0-9]+)\\s+([0-9]+):([0-9]+):([0-9]+)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(result);

			if (matcher.find()) {
				yearPU = Integer.parseInt(matcher.group(1));
				monthPU = Integer.parseInt(matcher.group(2));
				dayPU = Integer.parseInt(matcher.group(3));

				hourPU = Integer.parseInt(matcher.group(4));
				minutePU = Integer.parseInt(matcher.group(5));
				secondPU = Integer.parseInt(matcher.group(6));

				if (!displayMessage) displayDateTime();

				lastWsUpdate = System.currentTimeMillis();

				isWaitingForTime = false;

				systemRoutine();

				return;
			}
		}

		if (result != null && !result.isBlank())
		{
			displayResult(result);
		}

		systemRoutine();

	}

	public void displayResult(String result) {
		displayMessage = true;
		timerCounter = 2;

		setTextViewMessage(result);

		if (result.contains("fail") || result.contains(getString(R.string.str_operation_not_permitted))) {
			result = "⛔ " + result;

			Spannable wordToSpan = new SpannableString(result);
			wordToSpan.setSpan(new ForegroundColorSpan(getColor(R.color.colorError)), 0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			displayMessage = true;
			timerCounter = 5;

			setTextViewMessage(wordToSpan);
		}
	}

	private void requestPassword() {

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				onUserNameUpdated();

				final TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);

				String textContent = textViewUserName.getText().toString();

				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);
				builder.setIcon(R.drawable.ic_icon_account);
				builder.setTitle(textContent.isBlank() ? getString(R.string.str_title_login) : getString(R.string.str_title_change_user));

				// Set up the input
				final EditText input = new EditText(new ContextThemeWrapper(MainActivity.this, R.style.AppTheme_EditText));
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				input.setImeOptions(input.getImeOptions() | EditorInfo.IME_FLAG_FORCE_ASCII);
				input.setImeActionLabel(getString(R.string.Confirm), EditorInfo.IME_ACTION_DONE);
				input.setSingleLine();
				input.setText(currentPassword);
				input.setTypeface(getTypeface(Typeface.DEFAULT, false), Typeface.NORMAL);
				// input.setTypeface(getTypeface(Typeface.defaultFromStyle(Typeface.NORMAL), false));
				// input.setHint(R.string.str_enter_password);
				input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(CommonDefine.FCSTS_) });
				input.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
				input.setTextDirection(View.TEXT_DIRECTION_LTR);

				input.addTextChangedListener(new TextWatcher() {
					boolean deleting = false;
					int lastCount = 0;

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						// Nothing happens here
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						deleting = lastCount >= count;
					}

					@Override
					public void afterTextChanged(Editable editable) {
						if (!deleting) {
							String text = editable.toString();

						}
					}
				});

				// Create a toggle visibility Button
				Button toggleButton = new Button(MainActivity.this, null, R.style.ButtonStyle);
				toggleButton.setId(View.generateViewId());

				RelativeLayout.LayoutParams editTextParams = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				editTextParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				editTextParams.addRule(RelativeLayout.ALIGN_PARENT_START);
				// editTextParams.addRule(RelativeLayout.RIGHT_OF, toggleButton.getId());

				RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				buttonParams.addRule(RelativeLayout.ALIGN_TOP, input.getId());
				buttonParams.addRule(RelativeLayout.ALIGN_BOTTOM, input.getId());
				buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);
				buttonParams.addRule(RelativeLayout.LEFT_OF, toggleButton.getId());
				buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

				final int lineHeight = input.getLineHeight();
				final Typeface typeface = input.getTypeface();

				// Set OnClickListener for the button to toggle password visibility
				toggleButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						int selection = input.getSelectionEnd();
						boolean visible = input.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

						input.setInputType(visible ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD) : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
						input.setSelection(selection);
						input.requestFocus();
						input.setTypeface(typeface);

						Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
							v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
						}

						// toggleButton.setText(visible ? R.string.Show : R.string.Hide);

						Drawable drawable = getDrawable(visible ? R.drawable.ic_visibility_on : R.drawable.ic_visibility_off);
						drawable.setBounds( 0, 0, lineHeight, lineHeight );
						toggleButton.setCompoundDrawables(null, null, drawable, null);
					}
				});

				toggleButton.performClick();

				RelativeLayout inputGroup = new RelativeLayout(MainActivity.this);
				inputGroup.setLayoutParams(new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.MATCH_PARENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT));

				// Add the EditText and Button to the RelativeLayout
				inputGroup.addView(input, editTextParams);
				inputGroup.addView(toggleButton, buttonParams);

				// Create a LinearLayout as the root view
				LinearLayout rootLayout = new LinearLayout(MainActivity.this);
				rootLayout.setOrientation(LinearLayout.VERTICAL);

				final int alertDialogPadding = getDimensionFromAttribute(MainActivity.this, R.attr.dialogPreferredPadding, 64);

				rootLayout.setPadding(alertDialogPadding, 64, alertDialogPadding, 64);

				// Create a TextView
				TextView textView = new TextView(MainActivity.this);
				textView.setText(R.string.str_enter_password);

				textView.setTextColor(getColor(R.color.colorForeground));
				textView.setTextSize(16);
				textView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);

				rootLayout.addView(textView);
				rootLayout.addView(inputGroup);

				builder.setView(rootLayout);

				// Set up the positive and negative buttons
				builder.setPositiveButton(R.string.Confirm, null);
				builder.setNegativeButton(R.string.Cancel, null);

				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialogInterface) {
						InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
						inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
						inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);
						refreshConnectButton();
					}
				});

				builder.setOnKeyListener(new Dialog.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
							// Perform click on the negative button
							Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
							button.performClick();
							return true;
						}

						if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
							// Find the currently focused view
							View focusedView = ((AlertDialog) dialog).getCurrentFocus();
							if (focusedView != null) {
								// Perform click on the focused view
								focusedView.performClick();
								return true; // Indicate that the key event has been handled
							}
						}

						return false;
					}
				});

				InputFilter[] filters = new InputFilter[1];

				filters[0] = new InputFilter() {
					@Override
					public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
						if (end > start) {
							String destTxt = dest.toString();
							String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);

							resultingTxt = arabicToDecimal(resultingTxt);

							if (!resultingTxt.matches("^[\\x20-\\x7E]*$"))
								return "";

							return arabicToDecimal(source.subSequence(start, end).toString());
						}
						return null;
					}
				};

				input.setFilters(filters);

				input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						input.post(new Runnable() {
							@Override
							public void run() {
								InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
								inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
							}
						});
					}
				});

				input.requestFocus();

				AlertDialog dialog = builder.create();

				// DialogInterface.OnClickListener OnPositiveClick = new DialogInterface.OnClickListener() {
				View.OnClickListener OnPositiveClick = new View.OnClickListener() {
					@Override
					// public void onClick(DialogInterface dialog, int which) {
					public void onClick(View view) {
						String str = input.getText().toString().trim();

						if (str.isBlank())
							return;

						currentPassword = str;

						InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
						inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
						inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);

						dialog.dismiss();

						doLogin();
					}
				};

				// DialogInterface.OnClickListener OnNegativeClick = new DialogInterface.OnClickListener() {
				View.OnClickListener OnNegativeClick = new View.OnClickListener() {
					@Override
					// public void onClick(DialogInterface dialog, int which) {
					public void onClick(View view) {

						final Dialog mDialog = dialog;
						String text = input.getText().toString().trim();

						if (!text.isBlank() && !text.equals(currentPassword))
						{
							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle)
									.setIcon(R.drawable.ic_icon_question)
									.setTitle(R.string.str_title_cancel_login)
									.setMessage(R.string.str_cancel_login)
									.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											mDialog.cancel();
										}

									})
									.setNegativeButton(R.string.No, null);

							AlertDialog dialogQuestion = builder.create();

							dialogQuestion.show();

							return;
						}

						InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
						inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
						inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);

						dialog.cancel();
					}
				};

				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialogInterface) {
						Button buttonPositive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
						buttonPositive.setOnClickListener(OnPositiveClick);

						Button buttonNegative = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
						buttonNegative.setOnClickListener(OnNegativeClick);
					}
				});

				input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

						boolean isEnter = false;

						// If triggered by an enter key, this is the event; otherwise, this is null.
						if (keyEvent != null) {
							// if shift key is down, then we want to insert the '\n' char in the TextView;
							// otherwise, the default action is to send the message.
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
								if (keyEvent.isShiftPressed() && !input.isSingleLine()) {
									return false;
								}
							}

							isEnter = (keyEvent.getAction() == KeyEvent.ACTION_DOWN
									&& keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER);
						}

						// Identifier of the action. This will be either the identifier you supplied,
						// or EditorInfo.IME_NULL if being called due to the enter key being pressed.
						if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
							Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
							button.performClick();

							input.requestFocus();

							// Return true if you have consumed the action, otherwise false.
							return true;
						}

						return false;
					}
				});

				input.setOnKeyListener(new View.OnKeyListener() {
					@Override
					public boolean onKey(View view, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
							Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
							button.performClick();

							input.requestFocus();

							return true;
						}
						return false;
					}
				});

				// builder.show();

				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

				dialog.setCancelable(false);

				dialog.setCanceledOnTouchOutside(false);

				dialog.show();

				mDialog = dialog;

				refreshConnectButton();

				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
				}

				InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				// imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);

				input.postDelayed(new Runnable() {
					@Override
					public void run() {
						input.requestFocus();
						imm.showSoftInput(input, 0);
					}
				}, 500);

			}
		});

	}

	private void onUserNameUpdated() {
		final CardView cardViewUserViewIconCard = (CardView) findViewById(R.id.imageViewUserIconCard);

		final TextView textViewUserName = (TextView) findViewById(R.id.textViewUserName);

		final ImageView imageViewUserIcon = (ImageView) findViewById(R.id.imageViewUserIcon);

		textViewUserName.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);

		String textContent = textViewUserName.getText().toString();

		if (textContent.equals("--")) textContent = "";

		textViewUserName.setVisibility(textContent.isBlank() ? View.INVISIBLE : View.VISIBLE);

		// imageViewUserIcon.setVisibility(textViewUserName.getVisibility());

		cardViewUserViewIconCard.setVisibility(textViewUserName.getVisibility());

		// ImageViewCompat.setImageTintList(imageViewUserIcon, textViewUserName.getTextColors());
	}

	HttpRequest backgroundDateChecker = null;

	public void onDateTimeReceived(LocalDateTime localDateTime, String server) {
		if (localDateTime == null || server.isBlank()) {
			return;
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return;
		}

		if (server.equalsIgnoreCase("sffe")) {
			DeviceClockDiff = Math.abs(ChronoUnit.SECONDS.between(localDateTime, LocalDateTime.now()));
		}

		/*
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
					Toast toast = makeToast(MainActivity.this, String.format("Server: %s\nDevice: %s\nDiff: %s", localDateTime.toString(), LocalDateTime.now().toString(), DeviceClockDiff), R.drawable.ic_icon_clock, Toast.LENGTH_LONG);
					toast.show();
				}
			}
		});
		*/

	}

	private void systemRoutine() {
		if (isWsConnecting || isPerformingRequest || isUpdating || isTaring || mSocket == null || !mSocket.isOpen())
			return;

		TextView textViewUserName = ((TextView)findViewById(R.id.textViewUserName));
		String currentUser = textViewUserName.getText().toString();

		final boolean isNotSnRead = !currentUser.isBlank() && !((TextView) findViewById(R.id.textViewSerialNumber)).getText().toString().startsWith(getString(R.string.SN).trim());

		if (!isPuRead || (isNotSnRead && lastReadStatus == 0)) {
			String sUrl = String.format("http://%s/pu", serverAddress);
			new HttpRequest(MainActivity.this).execute(sUrl, "GET");
			return;
		}

		if (SystemLanguage.isBlank() || !isLanguageReceivedFromPu) {
			String sUrl = String.format("http://%s/lang", serverAddress);
			new HttpRequest(MainActivity.this).execute(sUrl, "GET");
			return;
		}

		if (currentUser.isBlank() && isPuRead) {
			String sUrl = String.format("http://%s/username", serverAddress);
			new HttpRequest(MainActivity.this).execute(sUrl, "GET");
			return;
		}

		if (ShowHiddenWeight == null) {
			String sUrl = String.format("http://%s/weight/display", serverAddress);
			new HttpRequest(MainActivity.this).execute(sUrl, "GET");
			return;
		}

		if (DeviceClockDiff == -1 && backgroundDateChecker == null) {
			backgroundDateChecker = new HttpRequest(MainActivity.this);
			backgroundDateChecker.inBackground = true;
			backgroundDateChecker.execute("https://www.gstatic.com/", "HEAD");
			return;
		}
	}

	private void doLogin() {
		// disconnect any open sockets first
		if (mSocket != null && mSocket.isOpen())
			mSocket.close();
		else
			mSocket = null;

		checkAvailInBackground = false;
		isWsConnecting = false;
		isPuRead = false;

		// refresh state
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				refreshConnectButton();
			}
		});


	}

	private void getReceipt() {

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle);
		builder.setIcon(R.drawable.ic_icon_information);
		builder.setTitle(R.string.str_title_receipt);
		final int max_rcpt_size = 1200000;

		// Set up the input
		final EditText input = new EditText(new ContextThemeWrapper(MainActivity.this, R.style.AppTheme_EditText));
		input.setInputType(InputType.TYPE_CLASS_NUMBER); // | InputType.TYPE_NUMBER_FLAG_UNSIGNED
		input.setImeOptions(input.getImeOptions() | EditorInfo.IME_FLAG_FORCE_ASCII);
		input.setImeActionLabel(getString(R.string.Confirm), EditorInfo.IME_ACTION_DONE);
		input.setSingleLine();
		input.setText("");
		// input.setHint(R.string.str_enter_receipt_number);
		input.setGravity(Gravity.CENTER);
		input.setTypeface(getTypeface(Typeface.DEFAULT, true));
		input.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
		input.setTextDirection(View.TEXT_DIRECTION_LTR);

		// Create a LinearLayout as the root view
		LinearLayout rootLayout = new LinearLayout(MainActivity.this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);

		final int alertDialogPadding = getDimensionFromAttribute(MainActivity.this, R.attr.dialogPreferredPadding, 64);

		rootLayout.setPadding(alertDialogPadding, 64, alertDialogPadding, 64);

		// Create a TextView
		TextView textView = new TextView(MainActivity.this);
		textView.setText(R.string.str_enter_receipt_number);

		textView.setTextColor(getColor(R.color.colorForeground));
		textView.setTextSize(16);
		textView.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.NORMAL);

		rootLayout.addView(textView);
		rootLayout.addView(input);

		builder.setView(rootLayout);

		// Set up the positive and negative buttons
		builder.setPositiveButton(R.string.Confirm, null);
		builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				refreshConnectButton();
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialogInterface) {
				PrintPaperInfo[0] = -1;

				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
				inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);

				refreshConnectButton();
			}
		});
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialogInterface) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
				inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);

				refreshConnectButton();
			}
		});

		builder.setOnKeyListener(new Dialog.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
					// Perform click on the negative button
					Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
					button.performClick();
					return true;
				}

				if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
					// Find the currently focused view
					View focusedView = ((AlertDialog) dialog).getCurrentFocus();
					if (focusedView != null) {
						// Perform click on the focused view
						focusedView.performClick();
						return true; // Indicate that the key event has been handled
					}
				}

				return false;
			}
		});

		input.addTextChangedListener(new TextWatcher() {
			boolean deleting = false;
			int lastCount = 0;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// Nothing happens here
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				deleting = lastCount >= count;
			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (!deleting) {
					String text = editable.toString();

					try {
						int num = Integer.parseInt(text);

						if (num > max_rcpt_size) {
							text = String.valueOf(max_rcpt_size);

							input.setText("");
							input.append(text);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		InputFilter[] filters = new InputFilter[1];

		filters[0] = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
				if (end > start) {
					String destTxt = dest.toString();
					String resultingTxt = destTxt.substring(0, dstart) +
							source.subSequence(start, end) +
							destTxt.substring(dend);

					resultingTxt = arabicToDecimal(resultingTxt);

					if (!resultingTxt.matches("^[0-9]+$") || resultingTxt.matches("^0+.*$"))
						return "";

					return arabicToDecimal(source.subSequence(start, end).toString());
				}
				return null;
			}
		};

		input.setFilters(filters);

		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				input.post(new Runnable() {
					@Override
					public void run() {
						InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
						inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
					}
				});
			}
		});

		input.requestFocus();

		String text = input.getText().toString();

		if (text.matches("^[0.:,+-]*$"))
			input.setSelection(0, text.length());

		AlertDialog dialog = builder.create();

		// DialogInterface.OnClickListener OnPositiveClick = new DialogInterface.OnClickListener() {
		View.OnClickListener OnPositiveClick = new View.OnClickListener() {
			@Override
			// public void onClick(DialogInterface dialog, int which) {
			public void onClick(View view) {
				boolean dismiss = true;
				String str = input.getText().toString().trim();

				input.requestFocus();

				if (!str.isBlank()) {
					str = arabicToDecimal(str);

					int num = 0;

					try {
						num = Integer.parseInt(str);

						if (num <= 0 || num > max_rcpt_size)
							throw new NumberFormatException(getString(R.string.str_invalid_number_specified));

						if (serverAddress == null || serverAddress.isBlank())
							return;

						PrintPaperInfo[1] = num;

						String sUrl = String.format("http://%s/receipt?id=%s", serverAddress, num);
						HttpRequest request = new HttpRequest(MainActivity.this);
						request.collectAsBulk = true;
						request.showProgress = true;
						request.execute(sUrl, "POST");
					} catch (NumberFormatException e) {
						e.printStackTrace();
						final String message = e.getMessage();

						if (!message.isBlank()) {
							// Toast toast = makeToast(MainActivity.this, message, R.drawable.ic_icon_error, Toast.LENGTH_SHORT);
							// toast.show();
						}

						dismiss = false;
					}

				} else {
					dismiss = false;

					String errorMessage = getString(R.string.str_enter_receipt_number).replaceAll(":", "") + ".";
					ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(getApplicationContext(), R.color.colorError));
					SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(errorMessage);
					spannableStringBuilder.setSpan(foregroundColorSpan, 0, errorMessage.length(), 0);
					Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_icon_error);
					drawable.setBounds(0, 0, input.getLineHeight(), input.getLineHeight());
					input.setError(spannableStringBuilder, drawable);

					// dialog.cancel();
					// refreshConnectButton();
					return;
				}

				if (dismiss) {
					InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
					inputMethodManager.hideSoftInputFromInputMethod(input.getWindowToken(), 0);
					dialog.dismiss();
					refreshConnectButton();
				}
			}
		};

		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
				button.setOnClickListener(OnPositiveClick);
			}
		});

		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

				boolean isEnter = false;

				// If triggered by an enter key, this is the event; otherwise, this is null.
				if (keyEvent != null) {
					// if shift key is down, then we want to insert the '\n' char in the TextView;
					// otherwise, the default action is to send the message.
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						if (keyEvent.isShiftPressed() && !input.isSingleLine()) {
							return false;
						}
					}

					isEnter = (keyEvent.getAction() == KeyEvent.ACTION_DOWN
							&& keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER);
				}

				// Identifier of the action. This will be either the identifier you supplied,
				// or EditorInfo.IME_NULL if being called due to the enter key being pressed.
				if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
					Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
					button.performClick();

					input.requestFocus();
					// Return true if you have consumed the action, otherwise false.
					return true;
				}

				return false;
			}
		});

		input.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View view, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
					Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
					button.performClick();

					input.requestFocus();

					return true;
				}
				return false;
			}
		});

		// builder.show();

		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		dialog.setCancelable(true);

		dialog.setCanceledOnTouchOutside(true);

		dialog.show();

		mDialog = dialog;

		refreshConnectButton();

		InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		// imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
		imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);

		input.postDelayed(new Runnable() {
			@Override
			public void run() {
				input.requestFocus();
				imm.showSoftInput(input, 0);
			}
		}, 200);

	}

	private void getReport() {

		if (PrintPaperInfo[0] != 2 || PrintPaperInfo[1] != 1)
			PrintPaperInfo[1] = -1;

		String whatType = "";

		switch (PrintPaperInfo[1]) {
			case -1:
				whatType = getString(R.string.str_start_date);
				break;
			case 1:
				whatType = getString(R.string.str_end_date);
				break;
		}

		if (!whatType.isBlank()) {
			String msg = String.format(getString(R.string.str_enter_item), whatType.trim());
			Toast toast = makeToast(getApplicationContext(), msg, R.drawable.ic_icon_information, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL | Gravity.FILL_HORIZONTAL, 0, 50);
			toast.show();
		}

		PersianDatePickerDialog picker = new PersianDatePickerDialog(this)
				.setTypeFace(getTypeface(Typeface.DEFAULT, true))
				.setBackgroundColor(getColor(R.color.colorBackground))
				.setPickerBackgroundColor(getColor(R.color.colorBackground))
				.setTitleColor(getColor(R.color.colorForeground))
				.setActionTextColor(getColor(R.color.colorForeground))
				.setPositiveButtonString(String.format(getString(R.string.str_submit_item), whatType))
				.setNegativeButton(getString(R.string.Cancel))
				.setTodayButton(getString(R.string.Today))
				.setTodayButtonVisible(false)
				.setMinYear(1380)
				.setMaxYear(PersianDatePickerDialog.THIS_YEAR)
				.setMaxMonth(12)
				// .setMaxDay(31)
				.setInitDate(PersianDatePickerDialog.THIS_YEAR, PersianDatePickerDialog.THIS_MONTH, PersianDatePickerDialog.THIS_DAY)
				.setActionTextColor(Color.BLACK)
				.setTypeFace(getTypeface(Typeface.DEFAULT, true))
				.setTitleType(PersianDatePickerDialog.NO_TITLE)
				.setShowInBottomSheet(false)
				.setAllButtonsTextSize(16)
				.setListener(new PersianPickerListener() {
					@Override
					public void onDateSelected(@NotNull PersianPickerDate persianPickerDate) {
						// Log.d(TAG, "onDateSelected: " + persianPickerDate.getTimestamp()); //675930448000
						// Log.d(TAG, "onDateSelected: " + persianPickerDate.getGregorianDate()); //Mon Jun 03 10:57:28 GMT+04:30 1991
						// Log.d(TAG, "onDateSelected: " + persianPickerDate.getPersianLongDate()); // دوشنبه 13 خرداد 1370
						// Log.d(TAG, "onDateSelected: " + persianPickerDate.getPersianMonthName()); //خرداد
						// Log.d(TAG, "onDateSelected: " + PersianCalendarUtils.isPersianLeapYear(persianPickerDate.getPersianYear())); //true
						// Toast.makeText(getApplicationContext(), persianPickerDate.getPersianYear() + "/" + persianPickerDate.getPersianMonth() + "/" + persianPickerDate.getPersianDay(), Toast.LENGTH_SHORT).show();
						// toast.cancel();

						PrintPaperInfo[0] = 2;

						switch (PrintPaperInfo[1]) {
							case -1:
								PrintPaperInfo[1] = 1;
								fromDate[0] = persianPickerDate.getPersianYear();
								fromDate[1] = persianPickerDate.getPersianMonth();
								fromDate[2] = persianPickerDate.getPersianDay();
								getReport();
								break;

							case 1:
								PrintPaperInfo[1] = -1;
								toDate[0] = persianPickerDate.getPersianYear();
								toDate[1] = persianPickerDate.getPersianMonth();
								toDate[2] = persianPickerDate.getPersianDay();

								String sUrl = String.format(
										"http://%s/report?fromYear=%s&fromMonth=%s&fromDay=%s&toYear=%s&toMonth=%s&toDay=%s",
										serverAddress, fromDate[0], fromDate[1], fromDate[2], toDate[0], toDate[1],
										toDate[2]);
								HttpRequest request = new HttpRequest(MainActivity.this);
								request.collectAsBulk = true;
								request.showProgress = true;
								request.execute(sUrl, "POST");
								break;

							default:
								PrintPaperInfo[1] = -1;
						}

					}

					@Override
					public void onDismissed() {
						PrintPaperInfo[0] = -1;
						PrintPaperInfo[1] = -1;
					}

				});

		picker.show();

		PrintPaperInfo[0] = -1;

	}

	private boolean isWsConnecting = false;

	public void initWsConnection() {

		if (serverAddress == null || serverAddress.isBlank()) {
			if (mSocket != null && mSocket.isOpen())
				mSocket.close();
			else
				mSocket = null;
			isWsConnecting = false;
			return;
		}

		if (mSocket == null)
			isWsConnecting = false;

		if (isWsConnecting) {
			Toast toast = makeToast(getApplicationContext(), R.string.str_another_connection_requested, R.drawable.ic_icon_wait, Toast.LENGTH_SHORT);
			toast.show();
			return;

			// isWsConnecting = false;
			// mSocket = null;

			// mSocket.reconnect();
			// return;
		}

		// || mSocket.isClosing() || mSocket.getReadyState() == ReadyState.NOT_YET_CONNECTED
		if (mSocket != null && (mSocket.isOpen())) {
			// Toast toast = makeToast(getApplicationContext(), R.string.str_cannot_make_new_connection, R.drawable.ic_icon_warning, Toast.LENGTH_SHORT);
			// toast.show();
			return;
		}

		try {
			String wsAddress = String.format("ws://%s/ws", serverAddress);

			mSocket = new SocketClient(wsAddress, MainActivity.this);

			if (mSocket != null && !mSocket.isClosing()) {
				if (mSocket.isOpen())
					mSocket.reconnect();

				else
					mSocket.connect();
			}

			isWsConnecting = true;

			setTextViewMessage(R.string.str_connecting_to_device);
		} catch (Exception e) {
			// URISyntaxException
			e.printStackTrace();

			mSocket = null;
		}

	}

	public void onWsConnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				isWsConnecting = false;

				TextView textViewWeight = findViewById(R.id.textViewWeight);
				textViewWeight.setText("PAND");

				if (isPuRead && (yearPU > 0 && monthPU > 0 && dayPU > 0)) displayDateTime();
				else isPuRead = false;

				displayMessage = true;
				timerCounter = 1;

				if (DeviceClockDiff == -1 && backgroundDateChecker != null) backgroundDateChecker = null;

				setTextViewMessage(R.string.str_connected);

				refreshConnectButton();

				systemRoutine();

				screenOn();
			}
		});
	}

	public void onWsDisconnected(final int code, final String reason, final boolean remote) {

		boolean isReconnecting = isUpdating || (checkAvailInBackground && isPerformingRequest), wasUpdating = isUpdating;

		yearPU = 0;
		monthPU = 0;
		dayPU = 0;
		hourPU = 0;
		minutePU = 0;
		secondPU = 0;
		isPuRead = false;
		isTaring = false;
		lastReadStatus = -1;
		isPerformingRequest = false;
		isWaitingForTime = false;
		isWsConnecting = false;
		isUpdating = false;
		lastWsUpdate = !isReconnecting ? 0 : System.currentTimeMillis();

		if (wasUpdating) {
			checkAvailInBackground = true;
			lastAvailCheck = System.currentTimeMillis();
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView textViewWeight = findViewById(R.id.textViewWeight),
						 textViewSerialNumber = findViewById(R.id.textViewSerialNumber);

				textViewWeight.setText(R.string.Weight);

				textViewSerialNumber.setText(R.string.SN);

				if (wasUpdating)         setTextViewMessage(R.string.str_rebooting);
				else if (isReconnecting) setTextViewMessage(R.string.str_connecting_to_device);
				else                     setTextViewMessage(R.string.Message);

				if (wasUpdating)
					textViewWeight.setText("PAND");

				if (serverAddress != null && !serverAddress.isBlank()) {
					textViewSerialNumber.setText(R.string.str_disconnected);

					String message = reason;

					if (!message.isBlank()) {
						if (message.equals("Host unreachable"))
							message = getString(R.string.str_unable_to_connect);

						if (message.toLowerCase().startsWith("failed to connect to"))
							message = getString(R.string.str_connection_failed);

						if (message.toLowerCase().startsWith("the connection was closed"))
							message = !isReconnecting ? getString(R.string.str_connection_closed) : "";

						if (message.toLowerCase().startsWith("invalid status code received"))
						{
							message = getString(R.string.str_connection_error);

							Pattern pattern = Pattern.compile("Invalid status code received: (\\d+) Status line: (.+)");
							Matcher matcher = pattern.matcher(reason);

							if (matcher.find()) {
								try {
									int statusCode = Integer.parseInt(matcher.group(1));
									String statusLine = matcher.group(2).replaceAll("HTTP/[\\d.]+", "");

									message += ":\n" + statusLine;
									if (handleHttpErrorCode(statusCode)) message = ""; // handled
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
						}

						if (!message.isBlank()) {
							Toast toast = makeToast(getApplicationContext(), message, R.drawable.ic_icon_error, Toast.LENGTH_LONG);
							toast.show();
						}

						textViewSerialNumber.setText(isReconnecting
								? getString(R.string.str_connecting_to_device)
								: getString(R.string.str_connection_error));

						textViewSerialNumber.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);
					}
				}

				textViewSerialNumber.setTypeface(
						textViewSerialNumber.getText().toString().startsWith(getString(R.string.SN).trim())
								? getTypeface(Typeface.MONOSPACE, false)
								: getTypeface(Typeface.DEFAULT, true)
						, Typeface.BOLD);

				refreshConnectButton();

			}
		});
	}

	private boolean handleHttpErrorCode(int statusCode) {
		// Toast.makeText(MainActivity.this, "Found code: " + statusCode, Toast.LENGTH_LONG).show();
		return false;
	}

	public void onWsError(final Exception e) {
		isWsConnecting = false;
		mSocket = null;

		// onWsDisconnected(0, e.getMessage(), false);
	}

	int ID_Index_PU = 0, IsExecutable_PU = 0;

	private boolean isUpdating = false;

	private boolean isTaring = false;

	private boolean isWaitingForTime = false;

	private boolean isPuRead = false;

	public int lastReadStatus = -1;

	Boolean ShowHiddenWeight = null;

	public void onWsUpdate(final String message) {

		lastWsUpdate = System.currentTimeMillis();

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				refreshConnectButton();

				String result = message.isBlank() ? "" : message.trim();

				if (result.equals("off"))
					result = "PAND";

				if (result.equals("power")) {
					if (serverAddress == null || serverAddress.isBlank())
						return;

					String sUrl = String.format("http://%s/power", serverAddress);
					new HttpRequest(MainActivity.this).execute(sUrl, "GET");
					return;
				}

				if (result.equals("rebooting")) {
					isPuRead = false;

					isUpdating = true; // signal for ws disconnect event
					isPerformingRequest = true; // wait until ws disconnect

					lastWsUpdate = System.currentTimeMillis();
					lastAvailCheck = System.currentTimeMillis();
					checkAvailInBackground = true; // must be true for isReconnecting
					yearPU = 0;
					monthPU = 0;
					dayPU = 0;

					timerCounter = 0;
					displayMessage = false;
					setTextViewMessage(R.string.str_rebooting);

					mSocket.close();

					result = "PAND";
					// return;
				}

				if (result.startsWith("update:")) {
					String[] parts = result.split(":");

					if (parts.length > 1) {
						String action = parts[1];
						if (action != null && !action.isBlank())
						{
							if (action.equalsIgnoreCase("start"))
							{
								/*
								isPuRead = false;
								yearPU = 0;
								monthPU = 0;
								dayPU = 0;

								// isUpdating = true;
								*/

								isPerformingRequest = true;

								displayMessage = true;
								timerCounter = 3;

								setTextViewMessage(R.string.str_update_started);
							}

							if (action.equalsIgnoreCase("end"))
							{
								isUpdating = false;
							}

							if (Pattern.compile("^\\d+%$").matcher(action).matches())
							{
								displayMessage = true;
								timerCounter = 3;

								setTextViewMessage(String.format(getString(R.string.str_updating), action));
							}

							refreshConnectButton();
						}
					}

					return;
				}

				if (result.startsWith("status:")) {
					final boolean wasPerformingRequest = isPerformingRequest || PrintPaperInfo[0] != -1;

					final TextView textViewSerialNumber = findViewById(R.id.textViewSerialNumber);

					if (lastReadStatus == 1 && isPerformingRequest && result.equalsIgnoreCase("status:0"))
						isPerformingRequest = false;

					if (!isPerformingRequest && lastReadStatus == 0 && result.equalsIgnoreCase("status:0"))
						return;

					Integer status = null;
					lastReadStatus = -1;

					try {
						status = Integer.parseInt(result.replace("status:", ""));
						switch (status) {
							case 0: isPuRead = false; break; // Ready
							case 1: isPuRead = false; break; // Busy
							case 2: break; // Booting
							default: isPuRead = false; break;
						}

						if (status != 0 && !wasPerformingRequest) {
							textViewSerialNumber.setText(getString(R.string.str_device_not_ready));
							textViewSerialNumber.setTypeface(getTypeface(Typeface.DEFAULT, true), Typeface.BOLD);
							isPuRead = false;
						}

						if (status == 1) isPerformingRequest = true;

						lastReadStatus = status;
					} catch (Exception e) {
						e.printStackTrace();
						isPuRead = false;
					}

					systemRoutine(); // get /pu status
					return;
				}

				if (result.startsWith("SN:")) {
					if (!isPuRead) return;
					TextView textViewSerialNumber = findViewById(R.id.textViewSerialNumber);
					try {
						final String serialNumber = result.replace("SN:", "").trim();
						textViewSerialNumber.setText(String.format("%s %s", getString(R.string.SN).trim(), serialNumber));
					} catch (Exception e) {
						e.printStackTrace();
						textViewSerialNumber.setText("????????");
					}
					textViewSerialNumber.setText(result.replace(":", ": "));
					textViewSerialNumber.setTypeface(getTypeface(Typeface.MONOSPACE, false), Typeface.BOLD);
					return;
				}

				if (result.startsWith("user:")) {
					TextView textViewUserName = findViewById(R.id.textViewUserName);
					try {
						final String currentUser = stringManagement(URLDecoder.decode(result.replace("user:", "").trim(), "ISO-8859-1")).trim();
						textViewUserName.setText(!currentUser.equals("--") ? currentUser : "");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						textViewUserName.setText(R.string.str_unknown_user);
					}
					onUserNameUpdated();
					return;
				}

				if (result.startsWith("level:")) {
					try {
						accessLevel = Integer.parseInt(result.replace("level:", ""));
					} catch (Exception e) {
						e.printStackTrace();
						accessLevel = 0;
					}
					onUserNameUpdated();
					return;
				}

				if (result.startsWith("lang:")) {
					String lang = result.replace("lang:", "");
					if (lang.length() == 1)
					{
						isLanguageReceivedFromPu = true;
						SystemLanguage = lang;
						UpdateAppLanguage();
					}
					else {
						Toast toast = makeToast(getApplicationContext(), lang.isBlank() ? getString(R.string.str_unable_to_process_language) : getString(R.string.str_language_format_invalid), R.drawable.ic_icon_warning, Toast.LENGTH_LONG);
						toast.show();
					}
					return;
				}

				if (result.startsWith("hidden:")) {
					String value = result.replace("hidden:", "");

					try {
						int num = Integer.parseInt(value);

						switch (num)
						{
							case 3: // No_
								ShowHiddenWeight = true;
								break;

							case 1: // No_
								ShowHiddenWeight = false;
								break;
						}

					} catch (Exception e) {
						e.printStackTrace();
						//String message = e.getMessage();
						//if (message != null && !message.isBlank()) {
						//    Toast toast = makeToast(getApplicationContext(), message, R.drawable.ic_icon_error, Toast.LENGTH_SHORT);
						//    toast.show();
						//}
					}

					refreshConnectButton();

					return;
				}

				if (result.startsWith("tare:")) {

					isTaring = result.endsWith(":" + CommonDefine.result_Processing);

					if (result.endsWith(":" + CommonDefine.result_Success))
						result = getString(R.string.str_request_completed);

					if (result.endsWith(":" + CommonDefine.result_Processing))
						result = getString(R.string.str_performing_request);

					if (!result.startsWith("tare:")) {
						displayMessage = true;
						timerCounter = 2;

						setTextViewMessage(result);
					}

					refreshConnectButton();

					return;

				}

				if (result.startsWith("beep:")) {
					String arg = result.replace("beep:", "");

					try {
						int num = Integer.parseInt(arg);

						if (num > 0) {

							Context context = getApplicationContext(); // MainActivity.this

							Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

							Ringtone ring = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);

							MediaPlayer mp = MediaPlayer.create(context, alarmSound);

							mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
								@Override
								public void onCompletion(MediaPlayer mp) {
									mp.reset();
									mp.release();
									mp = null;
								}
							});

							if (mp.isPlaying())
								mp.stop();

							// mp.start();

							// ring.play();

							NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "beep")
									.setSmallIcon(R.drawable.ic_logo_pand)
									.setContentTitle(getString(R.string.str_title_beep))
									.setContentText(getString(R.string.str_beep_received))
									.setSound(alarmSound)
									.setDefaults(Notification.DEFAULT_SOUND)
									.setCategory(Notification.CATEGORY_ALARM)
									.setColor(ContextCompat.getColor(context, R.color.colorAccent))
									.setPriority(NotificationCompat.PRIORITY_MAX)
									.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
									.setAutoCancel(true);

							/*
							NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
							bigText.bigText("Big Text");
							bigText.setBigContentTitle("Title");
							bigText.setSummaryText("Summary");

							notificationBuilder.setStyle(bigText);
							*/

							NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
							{
								String channelId = "beep";
								NotificationChannel channel = new NotificationChannel(
										channelId,
										"Beep Received from Indicator Device",
										NotificationManager.IMPORTANCE_HIGH);
								notificationManager.createNotificationChannel(channel);
								notificationBuilder.setChannelId(channelId);
							}

							Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
							notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
							notificationIntent.setAction(Intent.ACTION_MAIN);
							notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

							PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

							notificationBuilder.setContentIntent(pendingIntent);

							StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

							notificationManager.notify(1001, notificationBuilder.build());

							// for (StatusBarNotification notification : notifications) {
							//     if (notification.getId() == 1001) { ... }
							// }

							Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
								v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));
							} else {
								v.vibrate(100);
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
						if (e.getMessage() != null && !e.getMessage().isBlank()) {
							// Toast toast = makeToast(getApplicationContext(), e.getMessage(), R.drawable.ic_icon_error, Toast.LENGTH_SHORT);
							// toast.show();
						}
					}

					return;
				}

				if (result.startsWith("msg:")) {
					final String[] args = result.replace("msg:", "").split("," /*, 2*/);

					if (args.length > 1) {
						Context context = MainActivity.this; // getApplicationContext();

						try {
							args[1] = URLDecoder.decode(args[1], "ISO-8859-1").trim();

							final int iconId = Integer.parseInt(args[0]);
							final String msg = stringManagement(args[1]);

							Integer drawableId = null;

							switch (iconId) {
								case CommonDefine.ID_QuestionIcon_:    drawableId = R.drawable.ic_icon_question;    break;
								case CommonDefine.ID_MemoryIcon_:      drawableId = R.drawable.ic_icon_save;        break;
								case CommonDefine.ID_ErrorIcon_:       drawableId = R.drawable.ic_icon_error;       break;
								case CommonDefine.ID_WiFiIcon_:        drawableId = R.drawable.ic_icon_wifi;        break;
								case CommonDefine.ID_CautionIcon_:     drawableId = R.drawable.ic_icon_caution;     break;
								case CommonDefine.ID_DeleteIcon_:      drawableId = R.drawable.ic_icon_delete;      break;
								case CommonDefine.ID_InformationIcon_: drawableId = R.drawable.ic_icon_information; break;
								case CommonDefine.ID_WaitIcon_:        drawableId = R.drawable.ic_icon_wait;        break;
								default:
									// Handle the case when the iconId does not match any predefined values
									// drawableId = null;
									break;
							}

							Toast toast = makeToast(context, msg, drawableId, Toast.LENGTH_LONG);
							lastToastImage.setMinimumWidth(96);
							lastToastText.setPadding(48, 0, 48, 0);
							lastToastText.setTextSize(24);

							toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 50);

							Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							ringtone = RingtoneManager.getRingtone(context, notificationSoundUri);

							NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "beep")
									.setSmallIcon(drawableId)
									.setContentTitle(msg)
									// .setContentText(msg)
									.setSound(notificationSoundUri)
									.setCategory(Notification.CATEGORY_MESSAGE)
									.setColor(ContextCompat.getColor(context, R.color.colorAccent))
									.setPriority(NotificationCompat.PRIORITY_HIGH)
									.setAutoCancel(true);

							NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
							{
								String channelId = "message";
								NotificationChannel channel = new NotificationChannel(
										channelId,
										"Message Received from Indicator Device",
										NotificationManager.IMPORTANCE_HIGH);
								notificationManager.createNotificationChannel(channel);
								notificationBuilder.setChannelId(channelId);
							}

							Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
							notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
							notificationIntent.setAction(Intent.ACTION_MAIN);
							notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

							PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

							notificationBuilder.setContentIntent(pendingIntent);

							if (!hasWindowFocus() && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
								notificationManager.notify(1002, notificationBuilder.build());
							} else {
								toast.show();
								ringtone.play();
							}

							Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
								v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));
							} else {
								v.vibrate(100);
							}

							screenOn();
						}
						catch (Exception e) {
							e.printStackTrace();
							if (e.getMessage() != null && !e.getMessage().isBlank()) {
								// Toast toast = makeToast(getApplicationContext(), e.getMessage(), R.drawable.ic_icon_error, Toast.LENGTH_SHORT);
								// toast.show();
							}
						}
					}

					return;
				}

				if (result.startsWith("page:")) {
					result = result.replace("page:", "");

					try {
						ID_Index_PU = Integer.parseInt(result);
					} catch (Exception e) {
						e.printStackTrace();
						ID_Index_PU = -1;
					}

					refreshConnectButton();

					return;
				}

				if (result.startsWith("is_exec:")) {
					result = result.replace("is_exec:", "");

					try {
						IsExecutable_PU = Integer.parseInt(result);
					} catch (Exception e) {
						e.printStackTrace();
						IsExecutable_PU = 0;
					}

					refreshConnectButton();

					return;
				}

				Pattern pattern = Pattern.compile("([0-9]{4})/([0-9]+)/([0-9]+)\\s+([0-9]+):([0-9]+):([0-9]+)", Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(result);

				if (matcher.find()) {
					yearPU = Integer.parseInt(matcher.group(1));
					monthPU = Integer.parseInt(matcher.group(2));
					dayPU = Integer.parseInt(matcher.group(3));

					hourPU = Integer.parseInt(matcher.group(4));
					minutePU = Integer.parseInt(matcher.group(5));
					secondPU = Integer.parseInt(matcher.group(6));

					if (!displayMessage) displayDateTime();

					lastWsUpdate = System.currentTimeMillis();

					isWaitingForTime = false;

					return;
				}

				refreshConnectButton();

				if (isTaring)
					return;

				TextView textViewWeight = findViewById(R.id.textViewWeight);
				if (!textViewWeight.getText().equals(result))
					textViewWeight.setText(result);
			}
		});
	}

	private void UpdateAppLanguage() {

		SharedPreferences settings = getSharedPreferences("ClientInfo", 0);

		/*
		if (settings.contains("Language") && SystemLanguage.isBlank())
			SystemLanguage = settings.getString("Language", "");
		*/

		SharedPreferences.Editor editor = settings.edit();

		editor.putString("Language", SystemLanguage);

		if (!editor.commit()) {
			Toast toast = makeToast(getApplicationContext(), R.string.str_language_not_saved, R.drawable.ic_icon_warning, Toast.LENGTH_SHORT);
			toast.show();
		}

		ApplyAppLanguage();

	}

	private void ApplyAppLanguage() {

		String lang = SystemLanguage;

		if (lang.isBlank()) {
			SharedPreferences settings = getSharedPreferences("ClientInfo", 0);

			if (settings.contains("Language"))
				lang = settings.getString("Language", SystemLanguage);
		}

		if (lang.isBlank())
			return;

		String languageCode = "";

		switch (lang) {

			default:
				Toast toast = makeToast(getApplicationContext(), R.string.str_unsupported_language, R.drawable.ic_icon_warning, Toast.LENGTH_SHORT);
				toast.show();
				return;

			case "E":
				languageCode = "en";
				break;

			case "F":
				languageCode = "fa";
				break;

			case "A":
				languageCode = "ar";
				break;

		}

		Resources resources = this.getResources();
		Configuration config = resources.getConfiguration();
		Locale locale = Resources.getSystem().getConfiguration().locale;

		if (SystemLanguage.isBlank())
		{
			if (languageCode.isBlank()) {
				languageCode = locale.getLanguage();
			}

			if (languageCode.equals("en")) SystemLanguage = "E";
			if (languageCode.equals("fa")) SystemLanguage = "F";
			if (languageCode.equals("ar")) SystemLanguage = "A";
		}

		if (SystemLanguage.equals("F") || SystemLanguage.equals("A")) {
			getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
		}
		else {
			getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
		}

		locale = new Locale(languageCode);
		Locale.setDefault(locale);

		// config.locale = locale;
		config.setLocale(locale);
		config.setLayoutDirection(locale);
		resources.updateConfiguration(config, resources.getDisplayMetrics());
		this.createConfigurationContext(config);

		String currentLang = getIntent().getStringExtra("currentLang");

		String displayLanguage = locale.getDisplayLanguage();

		if (!languageCode.equals(currentLang)) {
			Intent refresh = new Intent(this, MainActivity.class);
			refresh.putExtra("currentLang", languageCode);
			refresh.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | refresh.getFlags());
			refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			finish();
			startActivity(refresh);
		}

		else {
			systemRoutine();
		}

	}

	public void onHttpError(final Exception e, HttpRequest r) {
		if (!r.apiUrl.contains(serverAddress))
			return;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				String reason = e.getMessage();

				if (reason != null && !reason.isBlank())
				{
					String message = getString(R.string.str_request_failed) + "\n" + reason;

					if (reason.toLowerCase().startsWith("failed to connect to"))
						message = getString(R.string.str_connection_failed);

					if (!message.isBlank()) {
						Toast toast = makeToast(getApplicationContext(), message, R.drawable.ic_icon_error, Toast.LENGTH_LONG);
						toast.show();
					}
				}

				if (e instanceof ConnectException && isServerAvailable) {
					checkAvailInBackground = false;
					// TextView textViewServerAddress = findViewById(R.id.textViewServerAddr);
					// textViewServerAddress.performClick();

					// isPuRead = false;
				}
			}
		});
	}

	public void onHttpError(final int statusCode, final String responseMessage, final String url, HttpRequest request) {
		if (!url.contains(serverAddress))
			return;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String message = getString(R.string.str_request_failed) + "\n" + statusCode + " " + responseMessage;

				if (url.contains("/pu")) {
					isPuRead = false;
					checkAvailInBackground = false;
					if (mSocket != null && mSocket.isOpen())
						mSocket.close();
					else
						mSocket = null;
				}

				if (!request.urlConnection.getContentType().contains("text/html"))
				{
					if (url.contains("/receipt") || url.contains("/report")) {
						String Kind = getString(R.string.File);
						if (url.contains("/receipt")) Kind = getString(R.string.Receipt);
						if (url.contains("/report")) Kind = getString(R.string.Report);
						if (statusCode == 404)
							message = String.format(getString(R.string.str_specified_not_exist), Kind);
						if (statusCode == 403)
							message = getString(R.string.str_operation_not_permitted);
						if (statusCode == 500)
							message = getString(R.string.str_request_failed).replaceAll(":", "") + ".";

						displayResult(message);

						message = "";
					}
				}

				if (handleHttpErrorCode(statusCode)) message = ""; // handled

				if (!message.isBlank()) {
					Toast toast = makeToast(getApplicationContext(), message, R.drawable.ic_icon_error, Toast.LENGTH_LONG);
					toast.show();
				}

			}
		});
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		refreshConnectButton();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		// savedInstanceState.putInt("Position", myVideoView.getCurrentPosition());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// myVideoView.seekTo(savedInstanceState.getInt("Position"));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		// if ( (mSocket == null || !mSocket.isOpen()) && !isWsConnecting )
		// checkAddressServer(false);
		refreshConnectButton();

		screenOn();

		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mHandler != null && mRunnable != null)
			mHandler.removeCallbacks(mRunnable);

		if (mSocket != null && mSocket.isOpen())
			mSocket.close();
	}

	@Override
	public void onBackPressed() {
		final ImageView imageViewPrintPaper = (ImageView) findViewById(R.id.imageViewPrintPaper);

		if (imageViewPrintPaper.getVisibility() == View.VISIBLE) {
			imageViewPrintPaper.setBackgroundColor(Color.TRANSPARENT);
			imageViewPrintPaper.setVisibility(View.GONE);
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
				.setIcon(R.drawable.ic_icon_exit)
				.setTitle(R.string.str_title_exit_app)
				.setMessage(R.string.str_question_exit_app)
				.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setNegativeButton(R.string.No, null);

		AlertDialog dialog = builder.create();

		dialog.show();
	}

}
