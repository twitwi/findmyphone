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

//	private static final int LOCATION_REQUEST_TIMEOUT = 5000; // 1 second
//	private static final long USE_OLD_FIX_THRESHOLD = 0; // 0 milliseconds
	
	private static final int LOCATION_REQUEST_TIMEOUT = 1000 * 60 * 5; // 5 minutes
	private static final long USE_OLD_FIX_THRESHOLD = 1000 * 60 * 5; // 5 minutes
	private static final int GPS_UPDATE_INTERVAL = 1000 * 60 * 1; // 1 minutes
	private Context context;
	private boolean inSearch;
	private LocationManager locationManager;
	private String currentFromAddress;
	private TimeoutThread timeoutThread;
	private String currentProvider;

	public CommandProcessor(Context context) {
		this.context = context;
	}

	private void retreiveBestLocation(boolean networkOk) {
		currentProvider = null;
		if(inSearch) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "retrieveBestLocation begin (inSearch)");
			locationManager.removeUpdates(this);
		} else {
			Log.d(FindMyPhoneHelper.LOG_TAG, "retrieveBestLocation begin (NOT inSearch)");
		}
		// Check if we have an old location that will do
		Log.d(FindMyPhoneHelper.LOG_TAG, "Check for an acceptable last known location");
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		long threshold = Calendar.getInstance().getTimeInMillis() - USE_OLD_FIX_THRESHOLD;
		if(location != null && location.getTime() > threshold ) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "Found an acceptable last known location (GPS)");
			processLocation(location, LocationManager.GPS_PROVIDER); // Found an OK GPS location
		} else if(networkOk) { // Check if we have a current network location
			Log.d(FindMyPhoneHelper.LOG_TAG, "OK to use Network provider");
			if(!providerExists(LocationManager.NETWORK_PROVIDER)) {
				failLocationSearch();
			} else {
				Log.d(FindMyPhoneHelper.LOG_TAG, "Reading location from Network");
				location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if(location != null && location.getTime() > threshold) {
					Log.d(FindMyPhoneHelper.LOG_TAG, "Found an acceptable last known location (Network)");
					processLocation(location, LocationManager.NETWORK_PROVIDER); // Found an OK Network location
				} else {
					failLocationSearch();
					/*
					 * We need to fix this Looper.prepare() business
					 * Make sure the timeout-thread starts some kind of intent or activity that will run the requestLocationUpdates()
					 * Right now, let's remove this to make the app work at all
					Log.d(FindMyPhoneHelper.LOG_TAG, "Request Location Updates (Network)");
					inSearch = true;
					currentProvider = LocationManager.NETWORK_PROVIDER;
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
					timeoutThread = new TimeoutThread(this);
					timeoutThread.timeoutNetwork(LOCATION_REQUEST_TIMEOUT);
					*/
				}
			}
		} else { // Try to get GPS fix
			Log.d(FindMyPhoneHelper.LOG_TAG, "Trying to get GPS Fix");
			inSearch = true;
			currentProvider = LocationManager.GPS_PROVIDER;
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL, 0, this);
			timeoutThread = new TimeoutThread(this);
			timeoutThread.timeoutGps(LOCATION_REQUEST_TIMEOUT);
		}
	}

	private void failLocationSearch() {
		Log.d(FindMyPhoneHelper.LOG_TAG, "Failed to get location. Taking last known location even though it's old. Considering both GPS and network");
		Location gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(netLoc.getTime() > gpsLoc.getTime()) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "Failback to last known NETWORK");
			processLocation(netLoc, LocationManager.NETWORK_PROVIDER);
		} else {
			Log.d(FindMyPhoneHelper.LOG_TAG, "Failback to last known GPS");
			processLocation(gpsLoc, LocationManager.GPS_PROVIDER);			
		}
	}

	private boolean providerExists(String checkProvider) {
		List<String> plist = locationManager.getAllProviders();
		Log.d(FindMyPhoneHelper.LOG_TAG, "providerExists. Nr providers " + plist.size());
		boolean result = false;
		for (Iterator<String> iterator = plist.iterator(); iterator.hasNext();) {
			String provider = iterator.next();
			Log.d(FindMyPhoneHelper.LOG_TAG, "Checking providers... " + provider);
			if(checkProvider.equals(provider)) {
				result  = true;
				break;
			}
		}
		return result;
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "Location Changed " + currentProvider);
		locationManager.removeUpdates(this);
		processLocation(location, currentProvider);
	}

	private void processLocation(Location location, String provider) {
		inSearch = false;
		String txt = "";
		if(location == null) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "Failed to get location!");
			txt = "FindMyPhone: Couldn't retreive location via GPS or network. Gave up!";
		} else {
			double lat = location.getLatitude();
			double lon = location.getLongitude();
			Log.d(FindMyPhoneHelper.LOG_TAG, "Got fix! lat " + lat + ", long " + lon);
			txt = getSmsTextByLocation(location, provider);
		}
		if(currentFromAddress != null && currentFromAddress.length() > 0) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "Sending SMS response to " + currentFromAddress);
			Log.d(FindMyPhoneHelper.LOG_TAG, txt.length() + " " + txt);
			SmsManager smsManager = SmsManager.getDefault();
			// ********* SENDING SMS HERE *******
			smsManager.sendTextMessage(currentFromAddress, null, txt, null, null);
			// ********* SENDING SMS HERE *******
		} else {
			Log.d(FindMyPhoneHelper.LOG_TAG, "No SMS! " + txt);
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
			Log.d(FindMyPhoneHelper.LOG_TAG, "running geo.getFromLocation");
			List<Address> georesult = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if(georesult != null && georesult.size() > 0) {
				Address adr = georesult.get(0);
				Log.d(FindMyPhoneHelper.LOG_TAG, "Geocoder " + georesult.size());
				for(int t = 0; t < adr.getMaxAddressLineIndex() ; t++) {
					if(adr.getAddressLine(t) != null) {
						txt += " " + adr.getAddressLine(t);
					}
				}
//				if(adr.getLocality() != null) {
//					txt += " LOC: <" + adr.getLocality() + ">";
//				}
			} else {
				Log.d(FindMyPhoneHelper.LOG_TAG, "Couldn't find geo-location");
			}
		} catch (IOException e) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "getAdressFromLocation: Got IO Exception");
			e.printStackTrace();
		}
		return txt.trim();
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "Provider " + provider + " disabled!");
		if(LocationManager.GPS_PROVIDER.equals(provider)) {
			abortGpsSearch();
		}
	}

	void abortGpsSearch() {
		if(inSearch) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "AbortGPSSearch called. Removing listener (Current " + currentProvider + ")");
			inSearch = false;
			locationManager.removeUpdates(this);
			// Try Network fix instead
			Log.d(FindMyPhoneHelper.LOG_TAG, "Trying Network location");
			retreiveBestLocation(true);
		}
	}
	public void abortNetworkSearch() {
		if(inSearch) {
			Log.d(FindMyPhoneHelper.LOG_TAG, "AbortNetworkSearch called. Removing listener (Current " + currentProvider + ")");
			inSearch = false;
			locationManager.removeUpdates(this);
			failLocationSearch();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "Provider enabled " + provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "Change of status " + status);
		if(inSearch) {
			switch(status) {
			case LocationProvider.OUT_OF_SERVICE:
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.d(FindMyPhoneHelper.LOG_TAG, "Location Not available yet. Trying Network location (Current " + currentProvider + ")");
				inSearch = false;
				locationManager.removeUpdates(this);
				retreiveBestLocation(true);
				break;
			}
		}
	}

	public void processCommand(SmsMessage msg) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "processCommand from " + msg.getOriginatingAddress());
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
		Log.d(FindMyPhoneHelper.LOG_TAG, "Resetting ringer from " + prev + " to " + max);
		am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		am.setStreamVolume(AudioManager.STREAM_RING, max, 0);
	}
}
