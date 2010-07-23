package se.erikofsweden.findmyphone;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FindMyPhoneCheckPhoneNumber extends BroadcastReceiver {

	private static final int MAX_PHONE_NUMBER_LENGTH = 100;
	private CommandProcessor cmd;

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean active = pref.getBoolean("service_active", false);
		String sendToNumber = pref.getString("report_phone", "");
		if(active) {
			TelephonyManager tMgr =(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String nr = tMgr.getLine1Number();
			String lastNr = readLastNumber(context);
			Log.d("FindMyPhone", "Last number " + lastNr + ", current " + nr);
			saveLastNumber(context, nr);
			if(nr != null && !nr.equals(lastNr) && lastNr != null && lastNr.length() > 0) {
				Log.d("FindMyPhone", "Number change!");
				if(sendToNumber.length() > 0) {
					if(cmd == null) {
						cmd = new CommandProcessor(context);
					}
					cmd.processCommand("sim_change", sendToNumber);
				}
			}
		}
	}

	public static void saveLastNumber(Context context, String nr) {
		if(nr != null && nr.length() > 0) {
			try {
				FileOutputStream fos = context.openFileOutput("settings_current_phone", Context.MODE_PRIVATE);
				fos.write(nr.getBytes());
				fos.close();
				Log.d("FindMyPhone", "Saved last number " + nr);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String readLastNumber(Context context) {
		String lastNr = null;
		try {
			byte[] buf = new byte[MAX_PHONE_NUMBER_LENGTH + 1];
			FileInputStream fis = context.openFileInput("settings_current_phone");
			int off = 0;
			int count = 0;
			while(off < MAX_PHONE_NUMBER_LENGTH && (count = fis.read(buf, off, MAX_PHONE_NUMBER_LENGTH - off)) != -1) {
				off += count;
			}
			fis.close();
			lastNr = new String(buf, 0, off);
			Log.d("FindMyPhone", "Read last number " + lastNr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lastNr;
	}

}
