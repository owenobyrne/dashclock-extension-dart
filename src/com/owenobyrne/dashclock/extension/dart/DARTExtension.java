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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.owenobyrne.dashclock.extension.dart.model.ArrayOfObjStation;
import com.owenobyrne.dashclock.extension.dart.model.ObjStation;

public class DARTExtension extends DashClockExtension implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	public static final String PREF_NAME = "pref_name";
	private Timer myTimer;
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

	@Override
	protected void onInitialize(boolean isReconnect) {
		setUpdateWhenScreenOn(true);
		getStations();
		Log.i("DART", "onInitialize()1");
		
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
				.expandedTitle(
						stationLookup.get(closestStation).getStationDesc() + " ("
								+ formatter.format((double) closestStationDistance) + "m)")
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

		if (mCurrentLocation != null) {
			Log.i("DART", "Provider " + provider + " has been selected.");
			onLocationChanged(mCurrentLocation);
		} else {
			Log.i("DART", "null location");

		}

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
		Log.i("DART", "onInitialize()5");
		mCurrentLocation = mLocationClient.getLastLocation();
		Log.i("DART", "Cached location: " + mCurrentLocation.toString());
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

			// Publish the extension data update.
			publishUpdate(new ExtensionData()
					.visible(true)
					.icon(R.drawable.ic_irishrail)
					.status("17.07 (+2)")
					.expandedTitle(
							stationLookup.get(closestStation).getStationDesc() + " ("
									+ formatter.format((double) closestStationDistance) + "m)")
					.expandedBody(
							"17.09 (+2 mins) Connolly - Dundalk\n17.29 (+1 min) Pearse - Drogheda\n17.55 Pearse - Drogheda")
					.clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))));

		}

	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
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
