package se.erikofsweden.findmyphonere;

import android.util.Log;

public class TimeoutThread extends Thread implements Runnable {

	private CommandProcessor commandProcessor;
	private int gpsTimeout = 0;
	private int networkTimeout = 0;
	private boolean finish;

	public TimeoutThread(CommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
		this.gpsTimeout = 0;
		this.networkTimeout = 0;
	}

	public void timeoutGps(int timeout) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "TimeoutGps");
		this.gpsTimeout = timeout;
		this.start();
	}

	public void timeoutNetwork(int timeout) {
		Log.d(FindMyPhoneHelper.LOG_TAG, "TimeoutNetwork");
		this.networkTimeout = timeout;
		this.start();
	}

	@Override
	public void run() {
		finish = false;
		Log.d(FindMyPhoneHelper.LOG_TAG, "TimeoutThread run()");
		super.run();
		if(gpsTimeout > 0) {
			try {
				Log.d(FindMyPhoneHelper.LOG_TAG, "GPSTimeout sleeping " + gpsTimeout);
				Thread.sleep(gpsTimeout);
				Log.d(FindMyPhoneHelper.LOG_TAG, "GPSTimeout done sleeping");
			} catch (InterruptedException e) {
				if(!finish) {
					Log.d(FindMyPhoneHelper.LOG_TAG, "GPSTimeout caught interrupted exception");
					e.printStackTrace();
				}
			}
			if(!finish) {
				commandProcessor.abortGpsSearch();
			}
		}
		if(networkTimeout > 0) {
			try {
				Log.d(FindMyPhoneHelper.LOG_TAG, "NetworkTimeout sleeping " + networkTimeout);
				Thread.sleep(networkTimeout);
				Log.d(FindMyPhoneHelper.LOG_TAG, "NetworkTimeout done sleeping ");
			} catch (InterruptedException e) {
				if(!finish) {
					Log.d(FindMyPhoneHelper.LOG_TAG, "NetworkTimeout caught interrupted exception");
					e.printStackTrace();
				}
			}
			if(!finish) {
				commandProcessor.abortNetworkSearch();
			}
		}
		Log.d(FindMyPhoneHelper.LOG_TAG, "TimeoutThread run() done");
	}
	
	public void finish() {
		finish = true;
		Log.d(FindMyPhoneHelper.LOG_TAG, "TimeoutThread cleanup");
		this.interrupt();
	}


}
