package com.owenobyrne.dashclock.extension.dart;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.owenobyrne.dashclock.extension.dart.model.ArrayOfObjStation;
import com.owenobyrne.dashclock.extension.dart.model.ObjStation;

public class DARTExtension extends DashClockExtension implements LocationListener {
	private static final String TAG = "DARTExtension";
	public static final String PREF_NAME = "pref_name";
	private Timer myTimer;
	private LocationManager locationManager;
	private String provider;
	ArrayOfObjStation stations = null;
	private HashMap<String, ObjStation> stationLookup = new HashMap<String, ObjStation>();
	private String closestStation;
	private float closestStationDistance;
	DecimalFormat formatter = new DecimalFormat("#,###");

	
	@Override
	protected void onInitialize(boolean isReconnect) {
		setUpdateWhenScreenOn(true);
		getStations();
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//locationManager.requestLocationUpdates(provider, 2000, 1, this);

		
		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}

		}, 0, 60000);
		
	}

	@Override
	protected void onUpdateData(int reason) {
		// Get preference value.
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String name = sp.getString(PREF_NAME, getString(R.string.pref_name_default));

		

		// Publish the extension data update.
		publishUpdate(new ExtensionData()
					.visible(true)
					.icon(R.drawable.ic_irishrail)
					.status("17.07 (+2)")
					.expandedTitle(stationLookup.get(closestStation).getStationDesc() + 
							" (" + formatter.format((double)closestStationDistance) + "m)")
					.expandedBody(
							"17.09 (+2 mins) Connolly - Dundalk\n17.29 (+1 min) Pearse - Drogheda\n17.55 Pearse - Drogheda")
					.clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))));

	}
	
	private void getStations() {
		InputStream in;
		HttpURLConnection urlConnection = null;
		String xml = ""; // not "null" as then the string concatentation results
							// in a string beginning with the null character.

		try {
			URL url = new URL("http://api.irishrail.ie/realtime/realtime.asmx/getAllStationsXML");

			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream());
			byte[] contents = new byte[1024];
			int bytesRead = 0;

			while ((bytesRead = in.read(contents)) != -1) {
				xml += new String(contents, 0, bytesRead);
			}

			Serializer serializer = new Persister();
			stations = serializer.read(ArrayOfObjStation.class, xml);
			
			for (ObjStation o : stations.getObjStation()) {
				stationLookup.put(o.StationCode, o);
			}
			
			Log.i("DART", stations.getObjStation().get(5).getStationDesc());

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			urlConnection.disconnect();
		}


	}

	private void TimerMethod() {
		// This method is called directly by the timer
		// and runs in the same thread as the timer.
		
		Log.i("DART", "Timer!");
		Criteria criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, false);
		Location location = locationManager.getLastKnownLocation(provider);

		if (location != null) {
			Log.i("DART", "Provider " + provider + " has been selected.");
			onLocationChanged(location);
		} else {
			Log.i("DART", "null location");
			
		}
		
		// Publish the extension data update.
				publishUpdate(new ExtensionData()
							.visible(true)
							.icon(R.drawable.ic_irishrail)
							.status("17.07 (+2)")
							.expandedTitle(stationLookup.get(closestStation).getStationDesc() + " (" + formatter.format((double)closestStationDistance) + "m)")
							.expandedBody(
									"17.09 (+2 mins) Connolly - Dundalk\n17.29 (+1 min) Pearse - Drogheda\n17.55 Pearse - Drogheda")
							.clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))));

	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d("DART", "onLocationChanged with location " + location.toString());
		if (stations != null) {
			float minDistance = 100000;
			float[] results = new float[2];
			
			for (ObjStation o : stations.getObjStation()) {
				Location.distanceBetween(location.getLatitude(), location.getLongitude(),
						o.getStationLatitude(), o.getStationLongitude(), results);
				Log.d("DART", "Checking station "+ o.getStationDesc());
				if (results.length > 0) {
					//Log.d("DART", "Distance: " + results[0]);
					if (results[0] < minDistance) {
						minDistance = results[0];
						closestStation = o.getStationCode();
					}
				}
			}
			
			closestStationDistance = minDistance;
			
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}
}
