package com.heeere.gpsmoi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.List;

public class FindMyPhoneSettings extends PreferenceActivity {
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        { // check possible intercepting apps
            // https://stackoverflow.com/a/8037914/2297277
            Intent intent = new Intent("android.provider.Telephony.SMS_RECEIVED");
            List<ResolveInfo> infos = getPackageManager().queryBroadcastReceivers(intent, 0);
            for (ResolveInfo info : infos) {
                Log.d(FindMyPhoneHelper.LOG_TAG, "Receiver name:" + info.activityInfo.name + "; priority=" + info.priority);
            }
        }

        { // request permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.INTERNET,
                    }, code);
        }

        addPreferencesFromResource(R.xml.preferences);

        { // Hack: if empty, set to default
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String k = "format_text";
            if (pref.getString(k, "").length() == 0) {
                pref.edit().putString(k, getString(R.string.default_format_text)).apply();
            }
            setPreferenceScreen(null);
            addPreferencesFromResource(R.xml.preferences);
        }

    }

    final int code = 42000;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == code) {
            Log.d(FindMyPhoneHelper.LOG_TAG, Arrays.toString(permissions) + Arrays.toString(grantResults));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	if(preference.getKey().equals("test_command")) {
        	Log.d(FindMyPhoneHelper.LOG_TAG, "Pref clicked " + preference.getKey()); // + " = " + preference.getSharedPreferences().getString(preference.getKey(), ""));
			Intent intent = new Intent(this, LocationMessageService.class);
			intent.setData(Uri.parse("?destinationAddress=" + ""));
			startService(intent);
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
}
