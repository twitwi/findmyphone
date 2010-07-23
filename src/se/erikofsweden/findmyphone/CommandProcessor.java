package se.erikofsweden.findmyphone;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class CommandProcessor implements LocationListener {

	private static final int GPS_TIMEOUT = 1000 * 60; // 1 minute
	private static final int GPS_UPDATE_INTERVAL = 1000 * 60 * 5; // 5 minutes
	private static final long USE_OLD_FIX_THRESHOLD = 1000 * 60 * 5; // 5 minutes
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
		long threshold = Calendar.getInstance().getTimeInMillis() - USE_OLD_FIX_THRESHOLD;
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
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL, 0, this);
			gpsTimeout = new GpsTimeoutThread(this);
			gpsTimeout.timeoutGps(GPS_TIMEOUT);
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
				break;
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
		String txt = "";
		if(location == null) {
			Log.d("FindMyPhone", "Failed to get location!");
			txt = "FindMyPhone: Couldn't retreive location via GPS or network. Gave up!";
		} else {
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			Log.d("FindMyPhone", "Got fix! lat " + lat + ", long " + lon);
			txt = getSmsTextByLocation(location, provider);
		}
		if(currentFromAddress != null && currentFromAddress.length() > 0) {
			Log.d("FindMyPhone", "Sending SMS response to " + currentFromAddress);
			Log.d("FindMyPhone", txt.length() + " " + txt);
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage(currentFromAddress, null, txt, null, null);
		} else {
			Log.d("FindMyPhone", "No SMS! " + txt);
		}
		currentFromAddress = null;
	}

	private String getSmsTextByLocation(Location location, String provider) {
		float acc = location.getAccuracy();
		if(!location.hasAccuracy()) acc = -1;
		if(provider == null) {
			provider = "";
		}
		String txt = "FindMyPhone (Acc: " + acc + " - " + provider + ") ";
		txt += getAddressFromLocation(location);
		txt += getGmapsUrl(location);
		if(txt.length() > 160) {
			// Only send the neccesary info
			txt = "FindMyPhone " + getGmapsUrl(location);
		}
		return txt;
	}

	private String getGmapsUrl(Location location) {
		String acc = String.valueOf(location.getAccuracy());
		if(!location.hasAccuracy()) acc = "?";
		return " http://maps.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude() + "%20(Your%20Phone%20" + acc + "m)";
	}

	private String getAddressFromLocation(Location location) {
		String txt = "";
		Geocoder geo = new Geocoder(context);
		try {
			Log.d("FindMyPhone", "running geo.getFromLocation");
			List<Address> georesult = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if(georesult != null && georesult.size() > 0) {
				Address adr = georesult.get(0);
				Log.d("FindMyPhone", "Geocoder " + georesult.size());
				for(int t = 0; t < adr.getMaxAddressLineIndex() ; t++) {
					if(adr.getAddressLine(t) != null) {
						txt += " " + adr.getAddressLine(t);
					}
				}
//				if(adr.getLocality() != null) {
//					txt += " LOC: <" + adr.getLocality() + ">";
//				}
			} else {
				Log.d("FinMyPhone", "Couldn't find geo-location");
			}
		} catch (IOException e) {
			Log.d("FindMyPhone", "getAdressFromLocation: Got IO Exception");
			e.printStackTrace();
		}
		return txt.trim();
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
				Log.d("FindMyPhone", "Location Not available yet. Trying Network location");
				inSearch = false;
				locationManager.removeUpdates(this);
				retreiveBestLocation(true);
				break;
			}
		}
	}

	public void processCommand(SmsMessage msg) {
		Log.d("FindMyPhone", "processCommand from " + currentFromAddress);
		processCommand(msg.getMessageBody(), msg.getOriginatingAddress());
	}

	public void processCommand(String command, String fromAddress) {
		currentFromAddress = fromAddress;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		turnOnRinger();
		retreiveBestLocation(false);
	}

	private void turnOnRinger() {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int max = am.getStreamMaxVolume(AudioManager.STREAM_RING);
		int prev = am.getStreamVolume(AudioManager.STREAM_RING);
		Log.d("FindMyPhone", "Resetting ringer from " + prev + " to " + max);
		am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		am.setStreamVolume(AudioManager.STREAM_RING, max, 0);
	}
}
