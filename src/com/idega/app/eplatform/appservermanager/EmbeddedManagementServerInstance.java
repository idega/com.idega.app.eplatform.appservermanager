package com.idega.app.eplatform.appservermanager;

import java.io.File;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;

public class EmbeddedManagementServerInstance implements WebappInstance {

	int port = 8080;
	String hostname = "localhost";
	Server server;
	boolean started=false;
	private WebappStartedListener startedListener;
	
	public EmbeddedManagementServerInstance(File baseDir) {
		// TODO Auto-generated constructor stub
	}

	public String getMainAppURL() {
		// TODO Auto-generated method stub
		return "http://"+hostname+":"+port+"/admin";
	}

	public String getStatus() {
		// TODO Auto-generated method stub
		//return null;
		if(!isStarted()){
			return "Starting embedded server";
		}
		else{
			return "Started";
		}
	}

	public boolean isStarted() {
		// TODO Auto-generated method stub
		return started;
	}
	
	public void setStarted(boolean started){
		if(started){
			this.started=started;
			if(this.startedListener!=null){
				this.startedListener.notifyStarted(this);
			}
		}
		this.started=started;
	}

	public void setStartedListener(WebappStartedListener startedListener) {
		this.startedListener=startedListener;

	}

	public void start() {
		server = new Server();
		try {
			Connector defaultConnector = new SocketConnector();
			defaultConnector.setPort(this.port);
			server.setConnectors(new Connector[]{defaultConnector});
			
			HandlerCollection rootHandler = (HandlerCollection)server.getHandler();
			if(rootHandler==null){
				rootHandler = new HandlerCollection();
				rootHandler.setServer(server);
				server.setHandler(rootHandler);
			}
			//DefaultHandler defaultHandler = new DefaultHandler();
			//rootHandler.addHandler(defaultHandler);
			Context context = new Context(rootHandler,"/");
			context.setVirtualHosts(new String[]{this.hostname});
			//context.setContextPath("/rugl");
			
			context.addServlet(TestServlet.class, "/*");
			
			server.start();

			setStarted(true);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		start();
	}

}
