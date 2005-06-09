package com.idega.app.eplatform.appservermanager;

import java.io.File;
import java.net.URI;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class AppservermanagerPlugin extends AbstractUIPlugin {
	//The shared instance.
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
		super.start(context);
		System.out.println("starting appservermanager plugin");

		//get the installation dir of the app:
		String sBaseDirUrl = System.getProperty("osgi.install.area");
		File baseDir = new File(new URI(sBaseDirUrl));
		
		manager = new AppserverManager(baseDir);
		Thread starter = new Thread(manager);
		starter.start();
		
		System.out.println("started appservermanager plugin");
	}



	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
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

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.idega.app.eplatform.appservermanager", path);
	}

	public AppserverManager getManager() {
		return manager;
	}

	public static AppservermanagerPlugin getPlugin() {
		return plugin;
	}
}
