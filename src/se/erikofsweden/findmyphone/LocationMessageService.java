package se.erikofsweden.findmyphone;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

public class LocationMessageService extends Service {

	public static final String ACTION_SMS_MESSAGE = "se.erikofsweden.findmyphone.ACTION_SMS_MESSAGE";
	private CommandProcessor cmd = null;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if(intent.getAction() == null || intent.getAction().equals(ACTION_SMS_MESSAGE)) {
			// Find location and send an SMS to the provided phone number
			Uri data = intent.getData();
			String phoneNumber = data.getQueryParameter("destinationAddress");
			if(cmd  == null) {
				cmd = new CommandProcessor(this.getApplicationContext());
			}
			cmd.processCommand(null, phoneNumber);
		}
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

}
