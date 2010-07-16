package se.erikofsweden.findmyphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class FindMyPhoneSMSReceiver extends BroadcastReceiver {

	private static CommandProcessor cmd = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean active = pref.getBoolean("service_active", false);
		String secret = pref.getString("secret_text", "");
		if(active && secret.length() > 0) {
			Bundle bundle = intent.getExtras();        
	        if (bundle != null)
	        {
	            Object[] pdus = (Object[]) bundle.get("pdus");
	            for (int i = 0; i < pdus.length; i++) {
					SmsMessage msg = SmsMessage.createFromPdu((byte[])pdus[i]);
					String from = msg.getOriginatingAddress();
					String txt = msg.getMessageBody().toString();
					Log.d("FindMyPhone", "From " + from);
					Log.d("FindMyPhone", "Txt " + txt);
					if(txt.indexOf(secret) == 0) {
						Log.d("FindMyPhone", "Found secret text");
						if(cmd == null) {
							cmd = new CommandProcessor(context);
						}
						cmd.processCommand(msg);
					}
				}
	        }
		} else {
			Log.d("FindMyPhone", "Service not active");
		}
	}

}
