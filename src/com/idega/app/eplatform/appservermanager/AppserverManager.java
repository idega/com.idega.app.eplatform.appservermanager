package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.tomcat.Tomcat5xInstalledLocalContainer;
import org.codehaus.cargo.container.tomcat.TomcatExistingLocalConfiguration;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;
import org.codehaus.cargo.generic.deployer.DeployerFactory;
import org.codehaus.cargo.util.FileUtils;
import com.idega.eplatform.util.FileDownloader;

public class AppserverManager implements Runnable {

	File baseDir;
	File managerServerDir;
	File logfile;
	InstalledLocalContainer managerContainer;
	int serverPort = 8080;
	boolean started = false;
	private String status;
	private String managerContainerId="default";
	private String mainContextPath;
	private String webappName="demo";
	
	public AppserverManager(File baseDir){
		log(baseDir.toString());
		this.baseDir=baseDir;
		managerServerDir=new File(baseDir,"adminserver");
		logfile = new File(managerServerDir,"out.log");
	}
	
	public void run() {
		// TODO Auto-generated method stub
		log("Starting Appserver");
		//if(tomcatinstalled()){
		//	start();
		//}
		//else{
			//installtomcat();
			start();
		//}
		log("Started Appserver on "+getMainAppURL());
	}
	 
	private InstalledLocalContainer installtomcat() {
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
			
			//ZipEntry entry;
			installer.install();
			
			File home = installer.getHome();
			
			LocalConfiguration configuration = new TomcatExistingLocalConfiguration(home);
			//managerContainer = new Resin3xContainer();
			managerContainer = new Tomcat5xInstalledLocalContainer(configuration);
			//managerContainer = new Jetty4xEmbeddedContainer();
			setContainerSettings(managerContainer);
			managerContainer.setHome(installer.getHome());
			managerContainer.setTimeout(600000);
			
			//deployIWWebapp(managerContainer);
			
			
			//Configuration configuration = managerContainer.getConfiguration();
			//ConfigurationFactory factory = new DefaultConfigurationFactory();
			//Configuration configuration = factory.createConfiguration(managerContainer, ConfigurationFactory.STANDALONE);
			
			//configuration.setProperty(ServletPropertySet.PORT,Integer.toString(serverPort));
			//managerContainer.setConfiguration(configuration);
		
			return managerContainer;
			
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			throw new RuntimeException(e);
		}
		//catch(NullPointerException npe){
		//	npe.printStackTrace();
		//}
		
	}

	private File getManagerServerDir() {
		return managerServerDir;
		
	}

	protected void setContainerSettings(InstalledLocalContainer container){
		container.setOutput(logfile);
		
		Map systemprops = new HashMap();
		//systemprops.put("Xmx","128M");
		systemprops.put("java.awt.headless","true");
		String appserverBaseDir = getManagerServerDir().getAbsolutePath();
		//systemprops.put("user.dir",appserverBaseDir);
		
		container.setSystemProperties(systemprops);
		
		//deployIWWebapp(container);
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
		File war = new File(getDownloadDir(),webappName+".war");
		if(!war.exists()){
			//download the file if it doesn't exist:
			//download the file if it doesn't exist:
			FileDownloader grabber;
			try {
				war.createNewFile();
				grabber = new FileDownloader(new URL("http://repository.idega.com/maven/iw-applications/wars/"+webappName+"-SNAPSHOT.war"),war);
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
		
		String mainContextPath = getMainContextPath();
		
		if(mainContextPath!=null && mainContextPath.equals("/")){
			File rootWar = new File(getDownloadDir(),"ROOT.war");
			try {
				if(!rootWar.exists()){
					rootWar.createNewFile();
					FileUtils utils = new FileUtils();
					FileInputStream input = new FileInputStream(war);
					FileOutputStream output = new FileOutputStream(rootWar);
					utils.copy(input,output);
					input.close();
					output.close();
				}
				return rootWar;
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return war;
	}
	
	protected File getDownloadDir(){
		return baseDir;
	}
	
	
	protected void deployIWWebapp(InstalledLocalContainer container){
		
		DeployableFactory factory = new DefaultDeployableFactory();//container.getCapability().;
		WAR war = (WAR) factory.createDeployable(managerContainerId,getWarFile().getAbsolutePath(),DeployableType.WAR);//factory.createWAR(getWarFile().getAbsolutePath());
		//WAR war = factory.createWAR("/Users/tryggvil/Downloads/content.war");
		//war.setContext("");
		
		try {
			log("deploying war from "+war.getFile().toURL());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//DefaultConfigurationFactory cfactory = new DefaultConfigurationFactory();
		//StandaloneLocalConfiguration configuration = (StandaloneLocalConfiguration) cfactory.createConfiguration(managerContainerId, ConfigurationType.STANDALONE);
		//configuration.setProperty(ServletPropertySet.PORT,Integer.toString(serverPort));
		//container.setConfiguration(configuration);
		//configuration.addDeployable(war);
		
		DeployerFactory deployerfactory = new DefaultDeployerFactory();
		Deployer deployer = deployerfactory.createDeployer(container,DeployerType.LOCAL);
		
		String contextPath = getMainContextPath();
		if(contextPath==null){
			contextPath = "/"+war.getContext();
			setMainContextPath(contextPath);
		}
		
		//war.setContext(getMainContextPath());
		war.setContext(contextPath);
		deployer.deploy(war);
	}
	
	public void start() {
		log("starting tomcat");
		File installDir = getManagerServerDir();
		if(managerContainer==null){
			//managerContainer = new Resin3xContainer();
			LocalConfiguration conf=null;
			if(installDir.exists()){
				//DefaultConfigurationFactory cfactory = new DefaultConfigurationFactory();
				//conf = (ExistingLocalConfiguration) cfactory.createConfiguration(managerContainerId, ConfigurationType.EXISTING);
				//conf = new TomcatExistingLocalConfiguration(installDir);
				//managerContainer = new Tomcat5xInstalledLocalContainer(conf);
				managerContainer = createExistingContainer(installDir);
				setContainerSettings(managerContainer);
				managerContainer.start();
			}
			else{
				managerContainer = installtomcat();
				//managerServerDir.mkdir();
				setContainerSettings(managerContainer);
				managerContainer.start();

				storeContainerSettings(installDir,managerContainer);
				/*try {
					Thread.sleep(60*1000);
				}
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				deployIWWebapp(managerContainer);
			}
		}
		try{
			//managerContainer.start();
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
	
	private void storeContainerSettings(File baseInstallDir,InstalledLocalContainer managerContainer2) {

		
		Properties prop = new Properties();
		try {
			File homeDir = managerContainer2.getHome();
			String fullPath = homeDir.getPath();
			String baseInstallPath = baseInstallDir.getPath();
			if(!baseInstallPath.endsWith(File.separator)){
				baseInstallPath+=File.separator;
			}
			String relativePath = fullPath.substring(baseInstallPath.length(),fullPath.length());
			
			prop.setProperty("tomcat0.home.dir",relativePath);
			prop.store(new FileOutputStream(new File(baseInstallDir,"installation.properties")), null);
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	private InstalledLocalContainer createExistingContainer(File installDir) {
		
		File homeDir = installDir;
		
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(new File(installDir,"installation.properties")));
			String installationProp = prop.getProperty("tomcat0.home.dir");
			if(installationProp!=null){
				homeDir = new File(installDir,installationProp);
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		LocalConfiguration conf = new TomcatExistingLocalConfiguration(homeDir);
		managerContainer = new Tomcat5xInstalledLocalContainer(conf);
		managerContainer.setHome(homeDir);
		return managerContainer;
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
		String fileUri = "file:/idega/eplatform-app";
		//File baseDir = new File(new URI("file:/Applications/eclipse3.1M7"));
		File baseDir = new File(new URI(fileUri));
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
		//return "/content";
		//return "/demo";
		return mainContextPath;
	}
	
	public void setMainContextPath(String contextPath){
		mainContextPath=contextPath;
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
