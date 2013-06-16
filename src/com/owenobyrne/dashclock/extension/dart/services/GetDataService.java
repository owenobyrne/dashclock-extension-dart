package com.owenobyrne.dashclock.extension.dart.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class GetDataService extends IntentService {

	private int result = Activity.RESULT_CANCELED;

	public GetDataService() {
		super("GetDataService");
	}

	// Will be called asynchronously be Android
	@Override
	protected void onHandleIntent(Intent intent) {
		InputStream in;
		HttpURLConnection urlConnection = null;
		String xml = ""; // not "null" as then the string concatentation results
							// in a string beginning with the null character.

		String urlPath = intent.getStringExtra("apitocall");
		
		InputStream stream = null;
		try {

			URL url = new URL(urlPath);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream());
			byte[] contents = new byte[1024];
			int bytesRead = 0;

			while ((bytesRead = in.read(contents)) != -1) {
				xml += new String(contents, 0, bytesRead);
			}

			// Sucessful finished
			result = Activity.RESULT_OK;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		Bundle extras = intent.getExtras();
		if (extras != null) {
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.arg1 = result;
			msg.obj = xml;
			try {
				messenger.send(msg);
			} catch (android.os.RemoteException e1) {
				Log.w(getClass().getName(), "Exception sending message", e1);
			}

		}
	}

}
