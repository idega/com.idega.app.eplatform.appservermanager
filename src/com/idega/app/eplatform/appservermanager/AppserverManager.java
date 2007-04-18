package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.util.FileUtils;

import com.idega.eplatform.util.FileDownloader;

public class AppserverManager implements Runnable {
	
	private static final String TOMCAT_5_0_DOWNLOAD_URL = "http://apache.rhnet.is/dist/jakarta/tomcat-5/v5.0.28/bin/jakarta-tomcat-5.0.28.zip";
	private static final String TOMCAT_5_5_DOWNLOAD_URL = "http://www.apache.org/dist/tomcat/tomcat-5/v5.5.23/bin/apache-tomcat-5.5.23.zip";
	private static final String JBOSS_DOWNLOAD_URL = "http://heanet.dl.sourceforge.net/sourceforge/jboss/jboss-4.0.5.GA.zip";
	private static final String DATABASES_DIR_NAME = "databases";
	private static final String SEPERATOR = File.separator;
	private File applicationInstallDir;
	private File managerServerDir;
	private String logfile;
	private InstalledLocalContainer managerContainer;
	private int serverPort = 8080;
	boolean started = false;
	private String status;
	
	//private String managerContainerId="jboss4x";
	private String managerContainerId="tomcat5x";
	
	private String webAppContext;
	private String webappName="ROOT";
	private String webAppFolderPath;
	
	private boolean useJBoss = false;
	
	boolean usePlatform4 = true;
	private String snapshotVersion = "eplatform-4.0-SNAPSHOT.war";
	private boolean runInDebugMode = false;
	@SuppressWarnings("deprecation")
	private FileUtils fileUtil;

	
	@SuppressWarnings("deprecation")
	public AppserverManager(File baseDir){
		log(baseDir.toString());
		this.applicationInstallDir=baseDir;
		this.managerServerDir= new File(baseDir,"appserver");
		this.logfile = new File(managerServerDir,"out.log").toString();
		this.fileUtil = new FileUtils();
	}
	
	public void run() {
		log("Starting IdegaWeb ePlatform RCP");
			start();
		log("Started IdegaWeb ePlatform RCP on : "+getMainAppURL());
	}
	
	private boolean installApplicationServer() {
		log("Installing application server");
		File installDir = getManagerServerDir();
		if(!installDir.exists()){
			installDir.mkdir();
		}
		Installer installer;
		try {
			URL applicationServerUrl = getDownloadedApplicationServerFile().toURL();
			installer = new ZipURLInstaller(applicationServerUrl,installDir.toString());
			installer.install();
			
			setWebAppFolderPath(installer.getHome());	
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private File getManagerServerDir() {
		return managerServerDir;
		
	}

	@SuppressWarnings("deprecation")
	protected void setContainerSettings(InstalledLocalContainer container){
		container.setOutput(logfile);
		
		Map<String, String> systemprops = new HashMap<String, String>();
		systemprops.put("Xmx","256M");
		systemprops.put("java.awt.headless","true");
		systemprops.put("file.encoding","UTF-8");
		
		if(runInDebugMode ){
			//TODO get this to work
			systemprops.put("agent","");
			systemprops.put("runjdwp:transport","dt_socket,server=y,suspend=n,address=10041");
		}
		
		String databasePropertiesFileName = "db.properties.hsqldb";
		try {
			this.fileUtil.createDirectory(this.getApplicationInstallDir(), DATABASES_DIR_NAME);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		systemprops.put("idegaweb.db.properties",this.getApplicationInstallDir().getAbsolutePath()+SEPERATOR+"plugins"+SEPERATOR+"com.idega.app.eplatform_3.0.0"+SEPERATOR+databasePropertiesFileName);
		//systemprops.put("user.dir",appserverBaseDir);
		
		container.setSystemProperties(systemprops);
	}
	
	protected File getDownloadedApplicationServerFile(){
		File appserver = getAppServerFile();
		if(!appserver.exists()){
			//download the file if it doesn't exist:
			FileDownloader grabber;
			try {
				appserver.createNewFile();
				grabber = new FileDownloader(new URL(getAppServerDownloadURL()),appserver);
				Thread thread = new Thread(grabber);
				thread.start();
				while(!grabber.isDone()){
					log(grabber.getDownloadState());
					Thread.sleep(3000);
				}
				//todo show percentage
				log(grabber.getDownloadState());
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}
		return appserver;
	}

	protected String getAppServerDownloadURL() {
		if(usePlatform4){
			if(useJBoss){
				return JBOSS_DOWNLOAD_URL;
			}
			else{
				return TOMCAT_5_5_DOWNLOAD_URL;
			}
		}
		else{
			return TOMCAT_5_0_DOWNLOAD_URL;
		}
	}

	protected File getAppServerFile() {
		if(usePlatform4){
			if(useJBoss){
				return new File(getDownloadDir(),JBOSS_DOWNLOAD_URL.substring(JBOSS_DOWNLOAD_URL.lastIndexOf("/")+1));
			}
			else{
				return new File(getDownloadDir(),TOMCAT_5_5_DOWNLOAD_URL.substring(TOMCAT_5_5_DOWNLOAD_URL.lastIndexOf("/")+1));
			}
		}
		else{
			return new File(getDownloadDir(),TOMCAT_5_0_DOWNLOAD_URL.substring(TOMCAT_5_0_DOWNLOAD_URL.lastIndexOf("/")+1));
		}
	}
	
	
	@SuppressWarnings("deprecation")
	protected File getWarFile(){
		File war = new File(getDownloadDir(),webappName+".war");
		//download the file if it doesn't exist:
		if(!war.exists()){
			FileDownloader grabber;
			try {
				
				war.createNewFile();
				grabber = new FileDownloader(new URL(getEPlatformDownloadURL()),war);
				
				Thread thread = new Thread(grabber);
				thread.start();
				while(!grabber.isDone()){
					log(grabber.getDownloadState());
					Thread.sleep(3000);
				}
				log(grabber.getDownloadState());
				
				String defaultTomcatRootWebapp = getWebAppFolderPath()+SEPERATOR+"webapps"+SEPERATOR+"ROOT";
				log("Deleting: "+defaultTomcatRootWebapp);
				//DELETE the default tomcat ROOT webapp, maybe we should just remove the whole webapps from the default tomcat download
				fileUtil.delete(new File(defaultTomcatRootWebapp));
				
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}
			
		return war;
	}

	protected String getEPlatformDownloadURL() {
		if(usePlatform4){
			return "http://repository.idega.com/maven2/com/idega/webapp/platform/eplatform/4.0-SNAPSHOT/"+snapshotVersion;

		}
		else{
			return "http://repository.idega.com/maven/iw-applications/wars/eplatform-3.1.60.war";
		}
	}
	
	protected File getDownloadDir(){
		return this.applicationInstallDir;
	}
	
	
	protected void deployIWWebapp(InstalledLocalContainer container){
		DeployableFactory factory = new DefaultDeployableFactory();
		WAR war = (WAR) factory.createDeployable(managerContainerId,getWarFile().getAbsolutePath(),DeployableType.WAR);//factory.createWAR(getWarFile().getAbsolutePath());
		
		String context = getWebAppContext();
		if(context!=null){
			war.setContext(context);
		}
		else{
			context ="/";
		}
		
		log("Deploying WAR file: "+this.webappName+ " to the context : "+context);
	
		
		container.getConfiguration().addDeployable(war);
		container.getConfiguration().configure(container);
	}
	
	public void start() {
		
		File installDir = getManagerServerDir();
		if(managerContainer==null){
			//install tomcat if neededd otherwise load existing app
			if(installDir.exists()){
				log("Application server found, configuring.");
				managerContainer = createExistingContainer(installDir.toString());
				setContainerSettings(managerContainer);
			}
			else{
				log("No application server found, downloading.");
				if(installApplicationServer()){
					log("Application server installed, configuring.");
					managerContainer = createExistingContainer(null);
				}
				else{
					log("Application server install FAILED, please quit and try again.");
				}
			}
			
			setContainerSettings(managerContainer);
			storeContainerSettings(installDir,managerContainer);
			
			//deploy if neccesery
			log("Deploying ePlatform");
			deployIWWebapp(managerContainer);
		}
		try{
//			then start
			managerContainer.start();
			setStarted(true);
			log("Application server started. Done");
		}
		catch(Exception e){
			log("Error starting application server - you could have a problem by not having a JDK properly installed");
			log(e.getMessage());
			if(e.getCause()!=null){
				log(e.getCause().getMessage());
			}
			e.printStackTrace();
		}
		
	}
	
	private void storeContainerSettings(File baseInstallDir,InstalledLocalContainer managerContainer2) {

		
		Properties prop = new Properties();
		try {
			
			String fullPath = managerContainer2.getHome();
			String baseInstallPath = baseInstallDir.getPath();
			if(!baseInstallPath.endsWith(SEPERATOR)){
				baseInstallPath+=SEPERATOR;
			}
			String relativePath = fullPath.substring(baseInstallPath.length(),fullPath.length());
			
			prop.setProperty("tomcat0.home.dir",relativePath);
			prop.store(new FileOutputStream(new File(baseInstallDir,"installation.properties")), null);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private InstalledLocalContainer createExistingContainer(String installDir) {
		String homeDir = getWebAppFolderPath();
		
		if(homeDir==null){
			Properties prop = new Properties();
			try {
				prop.load(new FileInputStream(new File(installDir,"installation.properties")));
				String installationProp = prop.getProperty("tomcat0.home.dir");
				log(installationProp);
				if(installationProp!=null){
					homeDir = installDir + SEPERATOR+installationProp;
					setWebAppFolderPath(homeDir);
				}
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//LocalConfiguration conf = new TomcatExistingLocalConfiguration(homeDir);
		
		
		ConfigurationFactory configurationFactory = new DefaultConfigurationFactory();
		LocalConfiguration configuration = (LocalConfiguration) configurationFactory.createConfiguration(managerContainerId, ContainerType.INSTALLED, ConfigurationType.EXISTING,homeDir);
//		configuration.setProperty(ServletPropertySet.PORT,Integer.toString(serverPort));
		
		managerContainer = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(managerContainerId, ContainerType.INSTALLED, configuration);
		//managerContainer = new Tomcat5xInstalledLocalContainer(conf);
		
		
		managerContainer.setHome(homeDir);
		managerContainer.setTimeout(600000);
		
		return managerContainer;
	}

	public void stop(){
		if(managerContainer!=null){
			log("stopping tomcat");
			managerContainer.stop();
		}
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
		
	public String getHostName(){
		return "127.0.0.1";
	}
	
	public String getMainAppURL(){
		return "http://"+getHostName()+":"+getServerPort()+((getWebAppContext()==null)?"/":getWebAppContext())+"workspace/site/";
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

	protected String getWebAppContext() {
		return webAppContext;
	}

	protected void setWebAppContext(String webAppContext) {
		this.webAppContext = webAppContext;
	}

	public boolean isRunningInDebugMode() {
		return runInDebugMode;
	}

	public void setToRunInDebugMode(boolean runInDebugMode) {
		this.runInDebugMode = runInDebugMode;
	}

	public File getApplicationInstallDir() {
		return applicationInstallDir;
	}

	public void setApplicationInstallDir(File applicationDir) {
		this.applicationInstallDir = applicationDir;
	}

	public String getWebAppFolderPath() {
		return webAppFolderPath;
	}

	public void setWebAppFolderPath(String webAppFolderPath) {
		this.webAppFolderPath = webAppFolderPath;
	}
}
