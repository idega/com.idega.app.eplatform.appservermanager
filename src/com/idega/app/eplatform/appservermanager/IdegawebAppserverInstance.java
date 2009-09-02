package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.configuration.StandaloneLocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.DatasourcePropertySet;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.util.FileUtils;

import com.idega.eplatform.util.FileDownloader;
import com.idega.manager.maven1.data.RepositoryLogin;
import com.idega.manager.maven2.RepositoryBrowserM2;

public class IdegawebAppserverInstance implements WebappInstance,Runnable {
	
	private static final String TOMCAT_5_0_DOWNLOAD_URL = "http://apache.rhnet.is/dist/jakarta/tomcat-5/v5.0.28/bin/jakarta-tomcat-5.0.28.zip";
	private static final String TOMCAT_5_5_DOWNLOAD_URL = "http://apache.tradebit.com/pub/tomcat/tomcat-5/v5.5.25/bin/apache-tomcat-5.5.25.zip";
	private static final String TOMCAT_6_0_DOWNLOAD_URL = "http://apache.dataphone.se/tomcat/tomcat-6/v6.0.20/bin/apache-tomcat-6.0.20.zip";
	private static final String JBOSS_DOWNLOAD_URL = "http://heanet.dl.sourceforge.net/sourceforge/jboss/jboss-4.0.5.GA.zip";
	
	private static final String JBOSS4_CONTAINER = "jboss4x";
	private static final String TOMCAT5_CONTAINER = "tomcat5x";
	private static final String TOMCAT6_CONTAINER = "tomcat6x";
	
	private static final String DATABASES_DIR_NAME = "data";
	private static final String APPSERVERS_DIR = "applications";
	private static final String EXTRA_LIB_DIR_NAME = "extra-lib";
	
	private static final String SEPERATOR = File.separator;
	boolean changeJavaHome=false;

	
	private File applicationInstallDir;
	private File managerServerDir;
	private File databasesDir;
	private File dataDir;
	private String logfile;
	private InstalledLocalContainer managerContainer;
	private int serverHttpPort = 9000;
	private int serverAjpPort = 9300;
	private int serverRMIPort = 9200;
	private String appserverId="tomcat0";
	boolean started = false;
	private String status;
	
	//private String managerContainerId=JBOSS4_CONTAINER;
	//private static String managerContainerId=TOMCAT5_CONTAINER;
	private static String managerContainerId=TOMCAT6_CONTAINER;
	
	private String webAppContext;
	private String webappName="ROOT";
	private String appserverHomePath;
	private String containerHomePath;
	
	//private boolean useJBoss = false;
	
	boolean usePlatform4 = true;

	//private static String currentVersion = "4.0.3-SNAPSHOT";
	//private static String currentVersionAndName = "felixclub-"+currentVersion+".war";
	//private static String currentVersionAndName = "felixclub-4.0.3-20080130.145328-1.war";
	
	private boolean runInDebugMode = false;
	@SuppressWarnings("deprecation")
	private FileUtils fileUtil;
	private File libDir;
	private File bundleDir;
	

	
	@SuppressWarnings("deprecation")
	public IdegawebAppserverInstance(File baseDir,File bundleDir){
		log(baseDir.toString());
		this.applicationInstallDir=baseDir;
		this.managerServerDir= new File(baseDir,APPSERVERS_DIR);
		this.databasesDir = new File(applicationInstallDir, DATABASES_DIR_NAME);
		this.bundleDir=bundleDir;
		this.libDir = getExtraLibsDir();
		this.logfile = new File(managerServerDir,this.getAppserverId()+"-out.log").toString();
		this.fileUtil = new FileUtils();
	}
	
	public void run() {
		log("Starting IdegaWeb ePlatform RCP");
			start();
		log("Started IdegaWeb ePlatform RCP on : "+getMainAppURL());
	}
	public void start() {
		
		File installDir = getManagerServerDir();
		boolean alreadyInstalled=false;
		//if(homeDir==null){
		if(installDir.exists()){
			alreadyInstalled = loadSettings(installDir, alreadyInstalled);
		}
		if(managerContainer==null){
			//install tomcat if neededd otherwise load existing app
			if(alreadyInstalled){
				log("Application server found, configuring.");
				managerContainer = createContainer(installDir,true);
			}
			else{
				log("No application server found, downloading.");
				if(downloadAndInstallApplicationServer()){
					log("Application server installed, configuring.");
					managerContainer = createContainer(installDir,false);
				}
				else{
					log("Application server install FAILED, please quit and try again.");
				}
			}
			
			
			setContainerSettings(managerContainer);

			if(!alreadyInstalled){
				deployIWWebapp(managerContainer);
			}
		}
		try{
//			then start
			String[] extraClasspath = getExtraClasspath();
			managerContainer.setExtraClasspath(extraClasspath);
			
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

	private boolean loadSettings(File installDir, boolean alreadyInstalled) {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(new File(installDir,"installation.properties")));
			String appServerHome = prop.getProperty(getAppserverId()+".home.dir");
			log("AppserverHome:"+appServerHome);
			if(appServerHome!=null){
				String homeDir = installDir + SEPERATOR+appServerHome;
				setAppserverHomePath(homeDir);
				alreadyInstalled=true;
			}
			String containerHome = prop.getProperty("container.home.dir");
			log("ContainerHome:"+containerHome);
			if(containerHome!=null){
				String containerHomeDir = installDir + SEPERATOR+containerHome;
				//setAppserverHomePath(containerHomeDir);
				setContainerHomePath(containerHomeDir);
			}
			String serverHttpPort = prop.getProperty(getAppserverId()+".http.port");
			if(serverHttpPort!=null){
				int port = Integer.parseInt(serverHttpPort);
				setServerHttpPort(port);
			}
		}
		catch (FileNotFoundException e) {
			//e.printStackTrace();
			//if(!installDir.exists()){
			//	installDir.mkdir();
			//}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return alreadyInstalled;
	}
	
	private boolean downloadAndInstallApplicationServer() {
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
			
			setContainerHomePath(installer.getHome());
			
			updateContainerLib(installer.getHome());
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private void updateContainerLib(String home) {
		
		File libDir = getLibDir();
		//Only works for tomcat6:
		File containerLib = new File(home,"lib");
		
		File[] libs = libDir.listFiles();
		for (int i = 0; i < libs.length; i++) {
			File lib = libs[i];
			if(lib.isFile()){
			File destinationFile = new File(containerLib,lib.getName());
			try {
				destinationFile.createNewFile();
				FileInputStream in = new FileInputStream(lib);			
				FileOutputStream out = new FileOutputStream(destinationFile);
				fileUtil.copy(in, out);
				in.close();
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			
		}
		
		
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
		

		getDataDir().mkdirs();
		String dataDir = getDataDir().getPath();
		if(changeJavaHome){
			systemprops.put("user.dir",dataDir);
			systemprops.put("user.home",dataDir);
		}
		if(runInDebugMode ){
			//TODO get this to work
			systemprops.put("agent","");
			systemprops.put("runjdwp:transport","dt_socket,server=y,suspend=n,address=10041");
		}
		
		//String databasePropertiesFileName = "db.properties.hsqldb";

		//this.fileUtil.createDirectory(this.getApplicationInstallDir(), DATABASES_DIR_NAME);

		
		//systemprops.put("idegaweb.db.properties",this.getApplicationInstallDir().getAbsolutePath()+SEPERATOR+"plugins"+SEPERATOR+"com.idega.app.eplatform_4.0.0"+SEPERATOR+databasePropertiesFileName);
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
			if(this.managerContainerId.equals(JBOSS4_CONTAINER)){
				return JBOSS_DOWNLOAD_URL;
			}
			else if(this.managerContainerId.equals(TOMCAT5_CONTAINER)){
				return TOMCAT_5_5_DOWNLOAD_URL;
			}
			else if(this.managerContainerId.equals(TOMCAT6_CONTAINER)){
				return TOMCAT_6_0_DOWNLOAD_URL;
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
			if(managerContainerId.equals(JBOSS4_CONTAINER)){
				return getLocalFileFromURL(JBOSS_DOWNLOAD_URL);
			}
			else if(managerContainerId.equals(TOMCAT5_CONTAINER)){
				return getLocalFileFromURL(TOMCAT_5_5_DOWNLOAD_URL);
			}
			else if(managerContainerId.equals(TOMCAT6_CONTAINER)){
				return getLocalFileFromURL(TOMCAT_6_0_DOWNLOAD_URL);
			}
			else{
				return getLocalFileFromURL(TOMCAT_5_5_DOWNLOAD_URL);
			}
		}
		else{
			return getLocalFileFromURL(TOMCAT_5_5_DOWNLOAD_URL);
		}
	}

	private File getLocalFileFromURL(String url) {
		return new File(getDownloadDir(),url.substring(url.lastIndexOf("/")+1));
	}
	
	
	@SuppressWarnings("deprecation")
	protected File getWarFile(){
		
		String warDownloadUrl = getWarDownloadURL();
		File war = getLocalFileFromURL(warDownloadUrl);
		//download the file if it doesn't exist:
		if(!war.exists()){
			FileDownloader grabber;
			try {
				
				war.createNewFile();
				grabber = new FileDownloader(new URL(warDownloadUrl),war);
				
				Thread thread = new Thread(grabber);
				thread.start();
				while(!grabber.isDone()){
					log(grabber.getDownloadState());
					Thread.sleep(3000);
				}
				log(grabber.getDownloadState());
				
				String defaultTomcatRootWebapp = getAppserverHomePath()+SEPERATOR+"webapps"+SEPERATOR+"ROOT";
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

	protected String getWarDownloadURL() {

		
		if(usePlatform4){
			String groupId = "com.idega.webapp.product";
			final String artifactId = "idega-webapp-base";
			try{
				RepositoryLogin login = RepositoryLogin.getInstanceWithoutAuthentication("http://repository.idega.com/maven2");
				RepositoryBrowserM2 browser = new RepositoryBrowserM2(login);
				
	
				//String artifactId = "felixclub";
				
				String url = browser.getArtifactUrlForMostRecent(groupId, artifactId);
				return url;
				//return "http://repository.idega.com/maven2/com/idega/webapp/custom/felixclub/"+currentVersion+"/"+currentVersionAndName;
			
			}
			catch(Exception e){
				File downloadDir=getDownloadDir();
				FileFilter filter = new FileFilter(){

					public boolean accept(File pathname) {
						if(pathname.getName().startsWith(artifactId)){
							return true;
						}
						return false;
					}
					
				};
				File[] files=downloadDir.listFiles(filter);
				if(files.length>0){
					URI uri =  files[0].toURI();
					String sURL=null;
					try {
						sURL = uri.toURL().toExternalForm();
					} catch (MalformedURLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					return sURL;
				}
				else {
					throw new RuntimeException("Cannot contact repository to download files");
				}
			}
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
			setWebAppContext(context);
			war.setContext(context);
		}
		
		log("Deploying WAR file: "+this.webappName+ " to the context : "+context);
	
		
		container.getConfiguration().addDeployable(war);
		container.getConfiguration().configure(container);
	}
	
	private String[] getExtraClasspath() {
		
		String jdbcDriverPath = getJdbcDriverPath();
		String[] paths = {jdbcDriverPath};
		// TODO Auto-generated method stub
		return paths;
	}

	public File getExtraLibsDir(){
		File baseBundlePath = this.bundleDir;
		if(baseBundlePath==null){
			String pluginFolder="com.idega.app.eplatform.appservermanager_1.0.0";
			baseBundlePath = new File(getApplicationInstallDir().getPath()+File.separator+"plugins"+File.separator+pluginFolder);
		}
		File libDir = new File(baseBundlePath,EXTRA_LIB_DIR_NAME);
		if(libDir.exists()){
				return libDir;
		}
		else{
			throw new RuntimeException("Libraries dir not found");
		}
	}
	
	private String getJdbcDriverPath() {
		String extraLibPath = getExtraLibsDir().getPath();
		String hsqlPath = extraLibPath+"hsqldb-1.8.0.2.jar";
		return hsqlPath;
	}

	private void storeContainerSettings(File baseInstallDir, String fullHomePath) {

		Properties prop = new Properties();
		try {
			
			//String fullHomePath = managerContainer2.getHome();
			String baseInstallPath = baseInstallDir.getPath();
			if(!baseInstallPath.endsWith(SEPERATOR)){
				baseInstallPath+=SEPERATOR;
			}
			if(fullHomePath!=null){
				String relativePath = fullHomePath.substring(baseInstallPath.length(),fullHomePath.length());
				prop.setProperty(getAppserverId()+".home.dir",relativePath);
			}
			String containerFullPath = getContainerHomePath();
			if(containerFullPath!=null){
				String containerRelativePath = containerFullPath.substring(baseInstallPath.length(),containerFullPath.length());
				prop.setProperty("container.home.dir",containerRelativePath);
			}
			
			String serverHttpPort = new Integer(getServerHttpPort()).toString();
			if(serverHttpPort!=null){
				prop.setProperty(getAppserverId()+".http.port",serverHttpPort);
			}
			
			String serverAjpPort = new Integer(getServerAjpPort()).toString();
			if(serverHttpPort!=null){
				prop.setProperty(getAppserverId()+".ajp.port",serverAjpPort);
			}
			
			String serverRmiPort = new Integer(getServerRMIPort()).toString();
			if(serverHttpPort!=null){
				prop.setProperty(getAppserverId()+".rmi.port",serverRmiPort);
			}
			
			prop.store(new FileOutputStream(new File(baseInstallDir,"installation.properties")), null);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private InstalledLocalContainer createContainer(File installDir,boolean alreadyInstalled) {
		
		//LocalConfiguration conf = new TomcatExistingLocalConfiguration(homeDir);
		
		
		ConfigurationFactory configurationFactory = new DefaultConfigurationFactory();
		LocalConfiguration configuration=null;
		if(alreadyInstalled){
			String homeDir = getAppserverHomePath();
			configuration = (LocalConfiguration) configurationFactory.createConfiguration(managerContainerId, ContainerType.INSTALLED, ConfigurationType.EXISTING,homeDir);
			
			String strServerPort = Integer.toString(getServerHttpPort());
			configuration.setProperty(ServletPropertySet.PORT,strServerPort);
		}
		else{
			
			File newCreatedHomeDir = new File(getManagerServerDir(),getAppserverId());
			//File newHomeDir = new File(getWebAppFolderPath());
			if(!newCreatedHomeDir.exists()){
				newCreatedHomeDir.mkdir();
			}
			String newCreatedHomeDirString = newCreatedHomeDir.getPath();
			
			configuration = (LocalConfiguration) configurationFactory.createConfiguration(managerContainerId, ContainerType.INSTALLED, ConfigurationType.STANDALONE,newCreatedHomeDirString);
			//configuration = (LocalConfiguration) configurationFactory.createConfiguration(managerContainerId, ContainerType.INSTALLED, ConfigurationType.STANDALONE);
			StandaloneLocalConfiguration standalone = (StandaloneLocalConfiguration) configuration;
			
			String dbName = getAppserverId();
			
			File hsqlDatabaseDir = new File(getDataDir(),"hsqldb");
			hsqlDatabaseDir.mkdirs();
			String databasePath = hsqlDatabaseDir.getPath()+File.separator+dbName;
			String dbUrl = "jdbc:hsqldb:file:"+databasePath;
			dbUrl = dbUrl.replace("\\", "\\\\");
			
			String datasource="cargo.datasource.url="+dbUrl+"|" +
					"cargo.datasource.driver=org.hsqldb.jdbcDriver|" +
					"cargo.datasource.username=sa|" +
					"cargo.datasource.password=|" +
					"cargo.datasource.type=javax.sql.DataSource|" +
					"cargo.datasource.jndi=jdbc/DefaultDS";
			configuration.setProperty(DatasourcePropertySet.DATASOURCE, datasource);
			
			/*configuration.setProperty("cargo.datasource.url", "jdbc:hsqldb:mem:idegaweb");
			configuration.setProperty("cargo.datasource.driver", "org.hsqldb.jdbcDriver");
			configuration.setProperty("cargo.datasource.username", "sa");
			configuration.setProperty("cargo.datasource.password", "");
			configuration.setProperty("cargo.datasource.type", "javax.sql.DataSource");
			configuration.setProperty("cargo.datasource.jndi", "jdbc/DefaultDS");*/
			
			configuration.setProperty(ServletPropertySet.PORT,Integer.toString(getServerHttpPort()));
			
			configuration.setProperty(GeneralPropertySet.RMI_PORT,Integer.toString(getServerRMIPort()));
			configuration.setProperty("cargo.ajp.port",Integer.toString(getServerAjpPort()));
			
			storeContainerSettings(installDir,newCreatedHomeDirString);

			//deploy if neccesery
			log("Deploying ePlatform Webapp");
		}
		
		managerContainer = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(managerContainerId, ContainerType.INSTALLED, configuration);
		//managerContainer = new Tomcat5xInstalledLocalContainer(conf);
		
		
		//managerContainer.setHome(homeDir);
		String containerHomeDir = getContainerHomePath();
		managerContainer.setHome(containerHomeDir);
		//managerContainer.setHome(installDir.toString());
		managerContainer.setTimeout(600000);
		
		return managerContainer;
	}

	private void initializePortsConfiguration() {
		//int defaultPort = 9000;
		//setServerHttpPort(defaultPort);
		
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

	private WebappStartedListener startedListener;

	
	public WebappStartedListener getStartedListener() {
		return startedListener;
	}

	public void setStartedListener(WebappStartedListener startedListener) {
		this.startedListener = startedListener;
	}

	public void setStarted(boolean started) {
		if(started){
			if(this.startedListener!=null){
				this.startedListener.notifyStarted(this);
			}
		}
		this.started = started;
	}
	
	
	public static void main(String args[])throws Exception{
		String fileUri = "file:/idega/eplatform-app";
		//File baseDir = new File(new URI("file:/Applications/eclipse3.1M7"));
		File baseDir = new File(new URI(fileUri));
		IdegawebAppserverInstance manager = new IdegawebAppserverInstance(baseDir,null);
		Thread thread = new Thread(manager);
		thread.start();
		while(true){
			//run endlessly:
			Thread.sleep(3000);
		}
		
	}

	public int getServerHttpPort(){
		return this.serverHttpPort;
	}
	
	public void setServerHttpPort(int serverPort){
		this.serverHttpPort=serverPort;
	}
		
	public String getHostName(){
		return "127.0.0.1";
	}
	
	public String getMainAppURL(){
		String initialAppPath = "workspace/";
		return "http://"+getHostName()+":"+getServerHttpPort()+((getWebAppContext()==null)?"/":getWebAppContext())+initialAppPath;
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

	public String getAppserverHomePath() {
		return appserverHomePath;
	}

	public void setAppserverHomePath(String fullPath) {
		this.appserverHomePath = fullPath;
	}

	public File getDataBaseDir() {
		return databasesDir;
	}

	public void setDataBaseDir(File databasesDir) {
		this.databasesDir = databasesDir;
	}
	
	public File getDataDir(){
		if(this.dataDir==null){
			this.dataDir=new File(getDataBaseDir(),getAppserverId());
		}
		return this.dataDir;
	}
	
	public void setDataDir(File dataDir){
		this.dataDir=dataDir;
	}

	private void setAppserverId(String appserverId) {
		this.appserverId = appserverId;
	}

	private String getAppserverId() {
		return appserverId;
	}
	

	public String getContainerHomePath() {
		return containerHomePath;
	}

	public void setContainerHomePath(String containerHomePath) {
		this.containerHomePath = containerHomePath;
	}

	public File getLibDir() {
		return libDir;
	}

	public void setLibDir(File libDir) {
		this.libDir = libDir;
	}

	public int getServerAjpPort() {
		return serverAjpPort;
	}

	public void setServerAjpPort(int serverAjpPort) {
		this.serverAjpPort = serverAjpPort;
	}

	public int getServerRMIPort() {
		return serverRMIPort;
	}

	public void setServerRMIPort(int serverRMIPort) {
		this.serverRMIPort = serverRMIPort;
	}

}
