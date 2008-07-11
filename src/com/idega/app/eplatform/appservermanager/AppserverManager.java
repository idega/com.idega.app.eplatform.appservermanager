package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppserverManager implements Runnable {

	private File baseDir;
	private List<WebappInstance> instances = new ArrayList<WebappInstance>();
	private File bundleLocation;

	public AppserverManager(File baseDir) {
		this(baseDir,null);
	}

	public AppserverManager(File baseDir, File bundleLocation) {
		this.baseDir=baseDir;
		this.bundleLocation=bundleLocation;
		createMainWebapp();
	}

	private WebappInstance createMainWebapp() {
		WebappInstance instance0 = new IdegawebAppserverInstance(this.baseDir,this.bundleLocation);
		//AppserverInstance instance0 = new EmbeddedManagementServerInstance(this.baseDir);
		getInstances().add(instance0);
		return instance0;
	}

	public void run() {
		getMainWebapp().start();
	}

	public void stop() {
		// TODO Auto-generated method stub
		for(WebappInstance instance: instances){
			instance.stop();
		}
	}

	public File getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	public List<WebappInstance> getInstances() {
		return instances;
	}

	public void setInstances(List<WebappInstance> instances) {
		this.instances = instances;
	}

	public WebappInstance getMainWebapp(){
		return getInstances().get(0);
	}
	
}
