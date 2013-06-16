package com.owenobyrne.dashclock.extension.dart.services;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.owenobyrne.dashclock.extension.dart.model.ArrayOfObjStation;
import com.owenobyrne.dashclock.extension.dart.model.ObjStation;

public class NearestDARTStationService extends Service implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
	private final IBinder mBinder = new MyBinder();
	private ArrayList<String> list = new ArrayList<String>();

	private LocationClient mLocationClient;
	private String provider;
	ArrayOfObjStation stations = null;
	private HashMap<String, ObjStation> stationLookup = new HashMap<String, ObjStation>();
	private String closestStation;
	private float closestStationDistance;
	DecimalFormat formatter = new DecimalFormat("#,###");
	Location mCurrentLocation;
	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 60;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 10;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;

	private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			String xml = (String)message.obj;

			if (message.arg1 == Activity.RESULT_OK && xml != null) {
				Toast.makeText(NearestDARTStationService.this, "Got Data", Toast.LENGTH_LONG)
						.show();
				Serializer serializer = new Persister();
				try {
					stations = serializer.read(ArrayOfObjStation.class, xml);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for (ObjStation o : stations.getObjStation()) {
					stationLookup.put(o.StationCode, o);
				}

				Log.i("DART", stations.getObjStation().get(5).getStationDesc());

			} else {
				Toast.makeText(NearestDARTStationService.this, "Data Download failed.",
						Toast.LENGTH_LONG).show();
			}

		};
	};

	@Override
	public void onCreate() {
		Log.i("DART", "onCreate()");

	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("DART", "onInitialize()1");

		// Load up information about the stations.
		getStationData();
		
		// can I connect to Google Play Services...
		servicesConnected();
		
		mLocationRequest = LocationRequest.create();
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 5 seconds
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		Log.i("DART", "onInitialize()2");

		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);
		Log.i("DART", "onInitialize()3");
		mLocationClient.connect();
		Log.i("DART", "onInitialize()4");

		return Service.START_STICKY;
	}

	void getStationData() {
		Intent intent = new Intent(this, GetDataService.class);
	    // Create a new Messenger for the communication back
	    Messenger messenger = new Messenger(handler);
	    intent.putExtra("MESSENGER", messenger);
	    intent.putExtra("apitocall", "http://api.irishrail.ie/realtime/realtime.asmx/getAllStationsXML");
	    startService(intent);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public class MyBinder extends Binder {
		public NearestDARTStationService getService() {
			return NearestDARTStationService.this;
		}
	}

	public List<String> getWordList() {
		return list;
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle dataBundle) {
		// Display the connection status
		Toast.makeText(this, "DART Extension Connected", Toast.LENGTH_SHORT).show();

		mLocationClient.requestLocationUpdates(mLocationRequest, this);
		mCurrentLocation = mLocationClient.getLastLocation();
		onLocationChanged(mCurrentLocation);

	}

	/*
	 * Called by Location Services if the connection to the location client
	 * drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "DART Extension Disconnected. Please re-connect.", Toast.LENGTH_SHORT)
				.show();
	}

	/*
	 * Called by Location Services if the attempt to Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Toast.makeText(this, "DART Extension Connection Failed " + connectionResult.getErrorCode(),
				Toast.LENGTH_SHORT).show();
	}

	// Define the callback method that receives location updates
	@Override
	public void onLocationChanged(Location location) {
		Log.d("DART", "onLocationChanged with location " + location.toString());
		if (stations != null) {
			float minDistance = 100000;
			float[] results = new float[2];

			for (ObjStation o : stations.getObjStation()) {
				Location.distanceBetween(location.getLatitude(), location.getLongitude(),
						o.getStationLatitude(), o.getStationLongitude(), results);
				// Log.d("DART", "Checking station " + o.getStationDesc());
				if (results.length > 0) {
					// Log.d("DART", "Distance: " + results[0]);
					if (results[0] < minDistance) {
						minDistance = results[0];
						closestStation = o.getStationCode();
					}
				}
			}

			closestStationDistance = minDistance;

		}

	}

	public String getNearestDARTStation() {
		return stationLookup.get(closestStation).getStationDesc() + " ("
				+ formatter.format((double) closestStationDistance) + "m)";
	}

	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("DART", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			Log.d("DART", "Google Play services is not available!");
			// Continue
			return false;
		}
	}

}
