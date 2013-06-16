package com.owenobyrne.dashclock.extension.dart.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.owenobyrne.dashclock.extension.dart.services.NearestDARTStationService;

/**
 * Called on Boot to kick off the Service.
 * 
 * @author Owen
 *
 */
public class StartServiceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, NearestDARTStationService.class);
		context.startService(service);
	}

}
