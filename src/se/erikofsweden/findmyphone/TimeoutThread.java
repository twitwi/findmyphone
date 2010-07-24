package se.erikofsweden.findmyphone;

public class TimeoutThread extends Thread implements Runnable {

	private CommandProcessor commandProcessor;
	private int gpsTimeout = 0;
	private int networkTimeout = 0;

	public TimeoutThread(CommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
		this.gpsTimeout = 0;
		this.networkTimeout = 0;
	}

	public void timeoutGps(int timeout) {
		this.gpsTimeout = timeout;
		this.start();
	}

	public void timeoutNetwork(int timeout) {
		this.networkTimeout = timeout;
		this.start();
	}

	@Override
	public void run() {
		super.run();
		if(gpsTimeout > 0) {
			try {
				Thread.sleep(gpsTimeout);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			commandProcessor.abortGpsSearch();
		}
		if(networkTimeout > 0) {
			try {
				Thread.sleep(networkTimeout);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			commandProcessor.abortNetworkSearch();
		}
	}


}
