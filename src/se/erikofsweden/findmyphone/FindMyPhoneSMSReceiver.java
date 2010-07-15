package se.erikofsweden.findmyphone;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.telephony.SmsManager;
import android.util.Log;

public class FindMyPhoneSMSReceiver extends BroadcastReceiver implements LocationListener {

	private LocationManager locationManager;
	private boolean inSearch = false;
	private String currentFromAddress;

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
						locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
						currentFromAddress = msg.getOriginatingAddress();
						retreiveBestLocation(false);
					}
				}
	        }
		} else {
			Log.d("FindMyPhone", "Service not active");
		}
	}

	private void retreiveBestLocation(boolean networkOk) {
		if(inSearch) {
			locationManager.removeUpdates(this);
		}
		// Check if we have an old location that will do
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		long threshold = Calendar.getInstance().getTimeInMillis() - 1000 * 60 * 5;
		if(location != null && location.getTime() > threshold ) {
			processLocation(location); // Found an OK GPS location
		} else if(networkOk) { // Check if we have a current network location
			location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if(location.getTime() > threshold) {
				processLocation(location); // Found an OK Network location
			}
		} else { // Try to get GPS fix
			Log.d("FindMyPhone", "Trying to get GPS Fix");
			inSearch = true;
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);			
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		processLocation(location);
		locationManager.removeUpdates(this);
	}

	private void processLocation(Location location) {
		inSearch = false;
		if(location == null) {
			Log.d("FindMyPhone", "Failed to get location!");
		} else {
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			float acc = location.getAccuracy();
			if(!location.hasAccuracy()) acc = -1;
			Log.d("FindMyPhone", "Got fix! lat " + lat + ", long " + lon + ", acc " + acc);
			if(currentFromAddress != null) {
				SmsManager smsManager = SmsManager.getDefault();
				String txt = "FindMyPhone found your phone here (Accuracy: " + acc + ")";
				txt += " http://maps.google.com/maps?q=" + lat + "," + lon + "%20(Your%20Phone%20is%20here)";
				smsManager.sendTextMessage(currentFromAddress, null, txt, null, null);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d("FindMyPhone", "Provider disabled!");
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d("FindMyPhone", "Provider enabled");
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d("FindMyPhone", "Change of status " + status);
		if(inSearch) {
			switch(status) {
			case LocationProvider.OUT_OF_SERVICE:
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.d("FindMyPhone", "Location Not available yet");
				inSearch = false;
				locationManager.removeUpdates(this);
				retreiveBestLocation(true);
				break;
			}
		}
	}
}
