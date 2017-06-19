package se.erikofsweden.findmyphonere;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class FindMyPhoneSettings extends PreferenceActivity {
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
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
