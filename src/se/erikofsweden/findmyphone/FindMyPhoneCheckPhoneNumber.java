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

	private CommandProcessor cmd;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("FindMyPhone", "Got a boot message!");
		TelephonyManager tMgr =(TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		String nr = tMgr.getLine1Number();
		Log.d("FindMyPhone", "Current phonenumber " + nr);
		String lastNr = readLastNumber(context);
		Log.d("FindMyPhone", "Last number " + lastNr);
		saveLastNumber(context, nr);
		if(nr != null && !nr.equals(lastNr)) {
			Log.d("FindMyPhone", "Number change!");
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			String sendToNumber = pref.getString("report_phone", "");
			if(sendToNumber.length() > 0) {
				if(cmd == null) {
					cmd = new CommandProcessor(context);
				}
				cmd.processCommand("sim_change", sendToNumber);
			}
		}
	}

	public static void saveLastNumber(Context context, String nr) {
		FileOutputStream fos;
		try {
			fos = context.openFileOutput("settings_current_phone", Context.MODE_PRIVATE);
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

	public static String readLastNumber(Context context) {
		String lastNr = null;
		try {
			byte[] buf = new byte[40];
			FileInputStream fis = context.openFileInput("settings_current_phone");
			int count = fis.read(buf); // TODO read settings in a better way
			fis.close();
			lastNr = new String(buf, 0, count);
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
