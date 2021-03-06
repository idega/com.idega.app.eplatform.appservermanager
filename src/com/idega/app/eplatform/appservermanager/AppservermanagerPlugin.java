package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.net.URI;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class AppservermanagerPlugin implements BundleActivator{// extends Plugin {
	//T he shared instance.
	private static AppservermanagerPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	private AppserverManager manager;

	
	/**
	 * The constructor.
	 */
	public AppservermanagerPlugin() {
		super();
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		//super.start(context);
		System.out.println("starting appservermanager plugin");

		//get the installation dir of the app:
		String sBaseDirUrl = System.getProperty("osgi.install.area");
		
		//Temp hardcoding:
		//sBaseDirUrl = "file:/idega/eplatform-app";
		//Specially replace the space:
		sBaseDirUrl = sBaseDirUrl.replaceAll(" ","%20");
		URI baseDirUri = new URI(sBaseDirUrl);
		File baseDir = new File(baseDirUri);
		String bundleLocation = context.getBundle().getLocation();
		if(bundleLocation.startsWith("initial@reference:")){
			bundleLocation = bundleLocation.substring("initial@reference:".length());
		}
		if(bundleLocation.startsWith("reference:")){
			bundleLocation = bundleLocation.substring("reference:".length());
		}

		URI bundleLocationURI = null;
		
		 
		if(bundleLocation.startsWith("file:")){
			String testBundleLocation = bundleLocation.substring("file:".length());
			//if the bundle URI starts with "plugins" it is a relative path so we need to add the eclipse platform prefix
			if(testBundleLocation.startsWith("plugins")){
				bundleLocation=sBaseDirUrl+testBundleLocation;
			}
		}
		try{
			bundleLocation = bundleLocation.replaceAll(" ","%20");
			bundleLocationURI = new URI(bundleLocation);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		//if(true){
		//	throw new RuntimeException("bundleLocationURI="+bundleLocationURI);
		//}
		File bundleLocationFolder = new File(bundleLocationURI);
		if(bundleLocationFolder.exists()){
			System.out.println("BundleFolder: "+bundleLocationFolder.getPath());
		}
		manager = new AppserverManager(baseDir,bundleLocationFolder);
		Thread starter = new Thread(manager);
		starter.start();
		
		System.out.println("started appservermanager plugin");
	}



	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		//super.stop(context);
		manager.stop();
		plugin = null;
		resourceBundle = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static AppservermanagerPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = AppservermanagerPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle("com.idega.app.eplatform.appservermanager.AppservermanagerPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

	public AppserverManager getManager() {
		return manager;
	}

	public static AppservermanagerPlugin getPlugin() {
		return plugin;
	}
}
