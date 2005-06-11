package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.zip.ZipEntry;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.configuration.ConfigurationFactory;
import org.codehaus.cargo.container.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.container.configuration.StandaloneConfiguration;
import org.codehaus.cargo.container.deployable.DeployableFactory;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat5xContainer;
import com.idega.eplatform.util.FileDownloader;

public class AppserverManager implements Runnable {

	File baseDir;
	File managerServerDir;
	File logfile;
	Container managerContainer;
	int serverPort = 8080;
	boolean started = false;
	private String status;
	
	public AppserverManager(File baseDir){
		log(baseDir.toString());
		this.baseDir=baseDir;
		managerServerDir=new File(baseDir,"adminserver");
		logfile = new File(managerServerDir,"out.log");
	}
	
	public void run() {
		// TODO Auto-generated method stub
		log("Starting Appserver");
		if(tomcatinstalled()){
			start();
		}
		else{
			installtomcat();
			start();
		}
		log("Started Appserver on "+getMainAppURL());
	}
	 
	private void installtomcat() {
		log("installing tomcat");
		File installDir = getManagerServerDir();
		if(!installDir.exists()){
			installDir.mkdir();
		}
		Installer installer;
		try {
			//installer = new ZipURLInstaller(new URL("http://www.caucho.com/download/resin-3.0.13.zip"),installDir);
			//installer = new ZipURLInstaller(new URL("file:///Users/tryggvil/Downloads/jakarta-tomcat-5.0.28.zip"),installDir);
			//installer = new ZipURLInstaller(new URL("http://heanet.dl.sourceforge.net/sourceforge/jetty/jetty-5.1.3-all.zip"),installDir);
			URL tomcatUrl = getDownloadedTomcatFile().toURL();
			installer = new ZipURLInstaller(tomcatUrl,installDir);
			
			ZipEntry entry;
			
			installer.install();

			//managerContainer = new Resin3xContainer();
			managerContainer = new Tomcat5xContainer();
			//managerContainer = new Jetty4xEmbeddedContainer();
			setContainerSettings(managerContainer);
			managerContainer.setHomeDir(installer.getHomeDir());
			managerContainer.setTimeout(600000);
			//Configuration configuration = managerContainer.getConfiguration();
			//ConfigurationFactory factory = new DefaultConfigurationFactory();
			//Configuration configuration = factory.createConfiguration(managerContainer, ConfigurationFactory.STANDALONE);
			
			//configuration.setProperty(ServletPropertySet.PORT,Integer.toString(serverPort));
			//managerContainer.setConfiguration(configuration);
		
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch(NullPointerException npe){
			npe.printStackTrace();
		}
	}

	private File getManagerServerDir() {
		return managerServerDir;
		
	}

	protected void setContainerSettings(Container container){
		container.setOutput(logfile);
		
		Map systemprops = new HashMap();
		//systemprops.put("Xmx","128M");
		systemprops.put("java.awt.headless","true");
		String appserverBaseDir = getManagerServerDir().getAbsolutePath();
		//systemprops.put("user.dir",appserverBaseDir);
		
		container.setSystemProperties(systemprops);
		
		deployIWWebapp(container);
	}
	
	protected File getDownloadedTomcatFile(){
		File appserver = new File(getDownloadDir(),"jakarta-tomcat-5.0.28.zip");
		if(!appserver.exists()){
			//download the file if it doesn't exist:
			//download the file if it doesn't exist:
			FileDownloader grabber;
			try {
				appserver.createNewFile();
				grabber = new FileDownloader(new URL("http://apache.rhnet.is/dist/jakarta/tomcat-5/v5.0.28/bin/jakarta-tomcat-5.0.28.zip"),appserver);
				Thread thread = new Thread(grabber);
				thread.start();
				while(!grabber.isDone()){
					log(grabber.getDownloadState());
					Thread.sleep(3000);
				}
				log(grabber.getDownloadState());
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return appserver;
	}
	
	
	protected File getWarFile(){
		File war = new File(getDownloadDir(),"content.war");
		if(!war.exists()){
			//download the file if it doesn't exist:
			//download the file if it doesn't exist:
			FileDownloader grabber;
			try {
				war.createNewFile();
				grabber = new FileDownloader(new URL("http://repository.idega.com/maven/iw-applications/wars/content-SNAPSHOT.war"),war);
				Thread thread = new Thread(grabber);
				thread.start();
				while(!grabber.isDone()){
					log(grabber.getDownloadState());
					Thread.sleep(3000);
				}
				log(grabber.getDownloadState());
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return war;
	}
	
	protected File getDownloadDir(){
		return baseDir;
	}
	
	
	protected void deployIWWebapp(Container container){
		
		DeployableFactory factory = container.getDeployableFactory();
		WAR war = factory.createWAR(getWarFile().getAbsolutePath());
		//WAR war = factory.createWAR("/Users/tryggvil/Downloads/content.war");
		//war.setContext("");
		
		try {
			log("deploying war from "+war.getFile().toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ConfigurationFactory cfactory = new DefaultConfigurationFactory();
		StandaloneConfiguration configuration = (StandaloneConfiguration) cfactory.createConfiguration(container, ConfigurationFactory.STANDALONE);
		configuration.setProperty(ServletPropertySet.PORT,Integer.toString(serverPort));
		container.setConfiguration(configuration);
		configuration.addDeployable(war);
		
		//DeployerFactory deployerfactory = new DefaultDeployerFactory();
		//Deployer deployer = deployerfactory.createDeployer(container,DeployerFactory.DEFAULT);
		//deployer.deploy(war);
		
	}
	
	public void start() {
		log("starting tomcat");
		File installDir = getManagerServerDir();
		if(managerContainer==null){
			//managerContainer = new Resin3xContainer();
			managerContainer = new Tomcat5xContainer();
			setContainerSettings(managerContainer);
		}
		try{
			managerContainer.start();
			log("started tomcat");
		}
		catch(Exception e){
			log("Error starting tomcat - you could have a problem by not having a JDK properly installed");
			log(e.getMessage());
			if(e.getCause()!=null){
				log(e.getCause().getMessage());
			}
			e.printStackTrace();
		}
		setStarted(true);
	}
	
	public void stop(){
		if(managerContainer!=null){
			log("stopping tomcat");
			managerContainer.stop();
		}
	}

	private boolean tomcatinstalled() {
		/*
		boolean isInstalled = getManagerServerDir().exists();
		if(isInstalled){
			log("tomcat is installed");
		}
		else{
			log("tomcat is not installed");			
		}
		return isInstalled;
		*/
		return false;
	}
	
	

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}
	
	
	public static void main(String args[])throws Exception{
		File baseDir = new File(new URI("file:/Applications/eclipse3.1M7"));
		AppserverManager manager = new AppserverManager(baseDir);
		Thread thread = new Thread(manager);
		thread.start();
		while(true){
			//run endlessly:
			Thread.sleep(3000);
		}
		
	}

	public int getServerPort(){
		return this.serverPort;
	}
	
	public String getMainContextPath(){
		//todo: remove hardcoding:
		return "/content";
	}
	
	public String getHostName(){
		return "localhost";
	}
	
	public String getMainAppURL(){
		return "http://"+getHostName()+":"+getServerPort()+getMainContextPath();
	}

	public void log(String logMessage){
		System.out.println(logMessage);
		setStatus(logMessage);
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
