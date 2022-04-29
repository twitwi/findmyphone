package com.heeere.gpsmoi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class FindMyPhoneHelper {

	private static final String SETTINGS_SIM_SERIAL = "settings_previous_sim_serial";
	public static final String LOG_TAG = "FindMyPhone";
	private static final int MAX_SIM_SERIAL_LENGTH = 100;

	public static String readPreviousSimSerialNumber(Context context) {
		String previousSerial = null;
		try {
			byte[] buf = new byte[MAX_SIM_SERIAL_LENGTH + 1];
			FileInputStream fis = context.openFileInput(SETTINGS_SIM_SERIAL);
			int off = 0;
			int count = 0;
			while(off < MAX_SIM_SERIAL_LENGTH && (count = fis.read(buf, off, MAX_SIM_SERIAL_LENGTH - off)) != -1) {
				off += count;
			}
			fis.close();
			previousSerial = new String(buf, 0, off);
			Log.d(FindMyPhoneHelper.LOG_TAG, "Read last SIM Serial " + previousSerial);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return previousSerial;
	}

	public static String savePreviousSimSerialNumber(Context context, String serial) {
		if(serial == null || serial.length() <= 0) {
			serial = "BLANK";
		}
		try {
			FileOutputStream fos = context.openFileOutput(SETTINGS_SIM_SERIAL, Context.MODE_PRIVATE);
			fos.write(serial.getBytes());
			fos.close();
			Log.d(FindMyPhoneHelper.LOG_TAG, "Saved last number " + serial);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return serial;
	}

}
