package se.erikofsweden.findmyphone;

public class GpsTimeoutThread extends Thread implements Runnable {

	private CommandProcessor commandProcessor;
	private int gpsTimeout;

	public GpsTimeoutThread(CommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
		this.gpsTimeout = 0;
	}

	public void timeoutGps(int timeout) {
		this.gpsTimeout = timeout;
		this.start();
	}

	@Override
	public void run() {
		super.run();
		if(gpsTimeout > 0) {
			try {
				Thread.sleep(30 * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			commandProcessor.abortGpsSearch();
		}
	}

}
