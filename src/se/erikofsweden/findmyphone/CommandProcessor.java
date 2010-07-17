package se.erikofsweden.findmyphone;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class CommandProcessor implements LocationListener {

	private Context context;
	private boolean inSearch;
	private LocationManager locationManager;
	private String currentFromAddress;
	private GpsTimeoutThread gpsTimeout;
	private String currentProvider;

	public CommandProcessor(Context context) {
		this.context = context;
	}

	private void retreiveBestLocation(boolean networkOk) {
		currentProvider = null;
		if(inSearch) {
			locationManager.removeUpdates(this);
		}
		// Check if we have an old location that will do
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		long threshold = Calendar.getInstance().getTimeInMillis() - 1000 * 60 * 5 * 0;
		if(location != null && location.getTime() > threshold ) {
			processLocation(location, LocationManager.GPS_PROVIDER); // Found an OK GPS location
		} else if(networkOk) { // Check if we have a current network location
			if(!providerExists(LocationManager.NETWORK_PROVIDER)) {
				Log.d("FindMyPhone", "Failed to get network location. Taking last known GPS location even though it's old");
				processLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), LocationManager.GPS_PROVIDER);
			} else {
				location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if(location != null && location.getTime() > threshold) {
					processLocation(location, LocationManager.NETWORK_PROVIDER); // Found an OK Network location
				} else {
					inSearch = true;
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
				}
			}
		} else { // Try to get GPS fix
			Log.d("FindMyPhone", "Trying to get GPS Fix");
			inSearch = true;
			currentProvider = LocationManager.GPS_PROVIDER;
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000 * 5, 0, this);
			gpsTimeout = new GpsTimeoutThread(this);
			gpsTimeout.timeoutGps(30 * 1000);
		}
	}

	private boolean providerExists(String checkProvider) {
		List<String> plist = locationManager.getAllProviders();
		Log.d("FindMyPhone", "providerExists " + plist.size());
		boolean result = false;
		for (Iterator<String> iterator = plist.iterator(); iterator.hasNext();) {
			String provider = iterator.next();
			Log.d("FindMyPhone", "Checking providers... " + provider);
			if(checkProvider.equals(provider)) {
				result  = true;
			}
		}
		return result;
	}

	@Override
	public void onLocationChanged(Location location) {
		processLocation(location, currentProvider);
		locationManager.removeUpdates(this);
	}

	private void processLocation(Location location, String provider) {
		inSearch = false;
		if(location == null) {
			Log.d("FindMyPhone", "Failed to get location!");
		} else {
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			float acc = location.getAccuracy();
			if(!location.hasAccuracy()) acc = -1;
			Log.d("FindMyPhone", "Got fix! lat " + lat + ", long " + lon + ", acc " + acc);
			String txt = "FindMyPhone found your phone here (Accuracy: " + acc + " - " + provider + ")";
			txt += " http://maps.google.com/maps?q=" + lat + "," + lon + "%20(Your%20Phone%20is%20here)";
			if(currentFromAddress != null) {
				SmsManager smsManager = SmsManager.getDefault();
				smsManager.sendTextMessage(currentFromAddress, null, txt, null, null);
			} else {
				Log.d("FindMyPhone", "No SMS! " + txt);
//				Toast.makeText(context, txt, Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d("FindMyPhone", "Provider " + provider + " disabled!");
		if(LocationManager.GPS_PROVIDER.equals(provider)) {
			abortGpsSearch();
		}
	}

	void abortGpsSearch() {
		if(inSearch) {
			Log.d("FindMyPhone", "AbortGPSSearch called. Removing listener");
			inSearch = false;
			locationManager.removeUpdates(this);
			// Try Network fix instead
			Log.d("FindMyPhone", "Trying Network location");
			retreiveBestLocation(true);
		}
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

	public void processCommand(SmsMessage msg) {
		currentFromAddress = msg.getOriginatingAddress();
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		retreiveBestLocation(false);
	}

	public void processCommand(String command) {
		currentFromAddress = null;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		retreiveBestLocation(false);		
	}
}
