package se.erikofsweden.findmyphone;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class LocationMessageService extends Service {

	public static final String ACTION_SMS_MESSAGE = "se.erikofsweden.findmyphone.ACTION_SMS_MESSAGE";
	private CommandProcessor cmd = null;
	private String currentPhoneNumber;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(FindMyPhoneHelper.LOG_TAG, "LocationMessageService:onCreate");		
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(FindMyPhoneHelper.LOG_TAG, "Inside LocationMessageService:onStart");
		if(intent.getAction() == null || intent.getAction().equals(ACTION_SMS_MESSAGE)) {
			// Find location and send an SMS to the provided phone number
			Uri data = intent.getData();
			Log.d(FindMyPhoneHelper.LOG_TAG, "UriData " + data.toString());			
			currentPhoneNumber = data.getQueryParameter("destinationAddress");
			if(cmd  == null) {
				Log.d(FindMyPhoneHelper.LOG_TAG, "Startar commandprocessor");
				cmd = new CommandProcessor(this.getApplicationContext(), intent);
			}
			cmd.processCommand(null, currentPhoneNumber);
			Thread sleepThread = new Thread() {
				public void run() {
					Log.d(FindMyPhoneHelper.LOG_TAG, "Thread:Run");
					try {
						sleep(CommandProcessor.LOCATION_REQUEST_TIMEOUT * 2);
						Log.d(FindMyPhoneHelper.LOG_TAG, "Thread:Interrupted!");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Log.d(FindMyPhoneHelper.LOG_TAG, "Thread:Done");
					doneWithService();
				};
			};
			sleepThread.start();
		}
	}
	
	private void doneWithService() {
		stopSelf();
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(FindMyPhoneHelper.LOG_TAG, "LocationMessageService:onDestroy");		
	}

}
