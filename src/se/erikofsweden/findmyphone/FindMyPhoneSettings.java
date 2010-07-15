package se.erikofsweden.findmyphone;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class FindMyPhoneSettings extends PreferenceActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}