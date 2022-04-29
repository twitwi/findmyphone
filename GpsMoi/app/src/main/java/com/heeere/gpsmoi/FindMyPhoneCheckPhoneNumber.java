package com.heeere.gpsmoi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FindMyPhoneCheckPhoneNumber extends BroadcastReceiver {

	private CommandProcessor cmd;

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean active = pref.getBoolean("service_active", false);
		String sendToNumber = pref.getString("report_phone", "");
		if(active) {
			TelephonyManager tMgr =(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			if(tMgr.getSimState() == TelephonyManager.SIM_STATE_PIN_REQUIRED) {
				// We should set up some kind of alarm and wait for like 5 mins to see if the SIM gets unlocked
				Log.d(FindMyPhoneHelper.LOG_TAG, "SIM is locked. Can't see if it has changed. Need new functionality to wait for SIM PIN unlock");
			}
			String currentSerial = tMgr.getSimSerialNumber();
			Log.d(FindMyPhoneHelper.LOG_TAG, "SIM Info: " + tMgr.getLine1Number() + ": " + tMgr.getSimSerialNumber() + ", " + tMgr.getSimState() + ", " + tMgr.getNetworkOperator() + ", " + tMgr.getSubscriberId());
			String previousSerial = FindMyPhoneHelper.readPreviousSimSerialNumber(context);
			Log.d(FindMyPhoneHelper.LOG_TAG, "Last number " + previousSerial + ", current " + currentSerial);
			// savePreviousSimSerialNumber may alter the serial to "BLANK" if it's null
			currentSerial = FindMyPhoneHelper.savePreviousSimSerialNumber(context, currentSerial);
			if(previousSerial != null && !previousSerial.equals(currentSerial) && previousSerial.length() > 0) {
				Log.d(FindMyPhoneHelper.LOG_TAG, "Number change!");
				if(sendToNumber.length() > 0) {
					if(cmd == null) {
						cmd = new CommandProcessor(context, intent);
					}
					cmd.processCommand("sim_change", sendToNumber);
				}
			}
		}
	}

}
