package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppserverManager implements Runnable {

	private File baseDir;
	private List<AppserverInstance> instances = new ArrayList<AppserverInstance>();

	public AppserverManager(File baseDir) {
		this.baseDir=baseDir;
		createMainAppserver();
	}

	private AppserverInstance createMainAppserver() {
		//AppserverInstance instance0 = new IdegawebAppserverInstance(this.baseDir);
		AppserverInstance instance0 = new EmbeddedManagementServerInstance(this.baseDir);
		getInstances().add(instance0);
		return instance0;
	}

	public void run() {
		getMainAppserver().start();
	}

	public void stop() {
		// TODO Auto-generated method stub
		for(AppserverInstance instance: instances){
			instance.stop();
		}
	}

	public File getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	public List<AppserverInstance> getInstances() {
		return instances;
	}

	public void setInstances(List<AppserverInstance> instances) {
		this.instances = instances;
	}

	public AppserverInstance getMainAppserver(){
		return getInstances().get(0);
	}
	
}
