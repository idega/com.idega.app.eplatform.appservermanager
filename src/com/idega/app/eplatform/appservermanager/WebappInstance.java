package com.idega.app.eplatform.appservermanager;


public interface WebappInstance extends Runnable {

	public String getMainAppURL();
	public String getStatus();

	public boolean isStarted();

	public void setStartedListener(WebappStartedListener startedListener);

	public void start();

	public void stop();


}
