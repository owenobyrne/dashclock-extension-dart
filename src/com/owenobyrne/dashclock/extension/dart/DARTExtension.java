package com.owenobyrne.dashclock.extension.dart;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.owenobyrne.dashclock.extension.dart.services.NearestDARTStationService;

public class DARTExtension extends DashClockExtension {
	private NearestDARTStationService nearestDARTStationService;
	public static final String PREF_NAME = "pref_name";
	private Timer myTimer;

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			nearestDARTStationService = ((NearestDARTStationService.MyBinder) binder).getService();
			Toast.makeText(DARTExtension.this, "Connected to service", Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			nearestDARTStationService = null;
		}
	};

	@Override
	protected void onInitialize(boolean isReconnect) {
		Log.i("DART", "Initializing...");
		setUpdateWhenScreenOn(true);
		Context context = this.getBaseContext();
		
		Intent service = new Intent(context, NearestDARTStationService.class);
		context.startService(service);
		
		bindService(new Intent(this, NearestDARTStationService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		
		myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new CheckServiceTask(), 0, 30*1000);
	

	}

	class CheckServiceTask extends TimerTask {
        public void run() {
        	Log.i("DART", "Timer!");

    		if (nearestDARTStationService != null) {
    			Log.i("DART", "Checking nearestDARTStationService.");

    			String nd = nearestDARTStationService.getNearestDARTStation();

    			publishUpdate(new ExtensionData()
    					.visible(true)
    					.icon(R.drawable.ic_irishrail)
    					.status("17.07 (+2)")
    					.expandedTitle(nd)
    					.expandedBody(
    							"17.09 (+2 mins) Connolly - Dundalk\n17.29 (+1 min) Pearse - Drogheda\n17.55 Pearse - Drogheda")
    					.clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))));

    		} else {
    			Log.i("DART", "null nearestDARTStationService");

    		}

        }
    }

	@Override
	protected void onUpdateData(int reason) {
		// Get preference value.
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String name = sp.getString(PREF_NAME, getString(R.string.pref_name_default));

		if (nearestDARTStationService != null) {
			Log.i("DART", "Checking nearestDARTStationService.");

			String nd = nearestDARTStationService.getNearestDARTStation();

			// Publish the extension data update.
			publishUpdate(new ExtensionData()
					.visible(true)
					.icon(R.drawable.ic_irishrail)
					.status("17.07 (+2)")
					.expandedTitle(nd)
					.expandedBody(
							"17.09 (+2 mins) Connolly - Dundalk\n17.29 (+1 min) Pearse - Drogheda\n17.55 Pearse - Drogheda")
					.clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))));
		} else {
			Log.i("DART", "null nearestDARTStationService");

		}
	}

}
