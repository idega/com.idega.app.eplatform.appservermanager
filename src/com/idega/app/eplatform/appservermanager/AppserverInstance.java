package com.idega.app.eplatform.appservermanager;


public interface AppserverInstance extends Runnable {

	public String getMainAppURL();
	public String getStatus();

	public boolean isStarted();

	public void setStartedListener(AppserverStartedListener startedListener);

	public void start();

	public void stop();


}
