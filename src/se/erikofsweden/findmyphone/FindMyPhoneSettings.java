package se.erikofsweden.findmyphone;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class FindMyPhoneSettings extends PreferenceActivity {
    private CommandProcessor cmd;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	Log.d("FindMyPhone", "Pref clicked " + preference.getKey()); // + " = " + preference.getSharedPreferences().getString(preference.getKey(), ""));
    	if(preference.getKey().equals("test_command")) {
			if(cmd == null) {
				cmd = new CommandProcessor(this.getApplicationContext());
			}
			cmd.processCommand("test", null);
    	}
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
}