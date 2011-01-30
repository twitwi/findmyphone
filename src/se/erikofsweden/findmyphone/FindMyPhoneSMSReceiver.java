package se.erikofsweden.findmyphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class FindMyPhoneSMSReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean active = pref.getBoolean("service_active", false);
		String secret = pref.getString("secret_text", "").toLowerCase();
		if(active && secret.length() > 0) {
			Bundle bundle = intent.getExtras();
	        if (bundle != null)
	        {
	            Object[] pdus = (Object[]) bundle.get("pdus");
	            for (int i = 0; i < pdus.length; i++) {
					SmsMessage msg = SmsMessage.createFromPdu((byte[])pdus[i]);
					String from = msg.getOriginatingAddress();
					String txt = msg.getMessageBody().toString();
					if(txt.toLowerCase().indexOf(secret) == 0) {
						Log.d(FindMyPhoneHelper.LOG_TAG, "Got SMS with secret text " + from);
						Intent locationIntent = new Intent(context, LocationMessageService.class);
						locationIntent.setData(Uri.parse("?destinationAddress=" + from));
						context.startService(locationIntent);
					}
				}
	        }
		}
	}

}
