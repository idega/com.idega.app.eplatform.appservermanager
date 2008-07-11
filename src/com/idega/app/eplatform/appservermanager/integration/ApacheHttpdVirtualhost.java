package com.idega.app.eplatform.appservermanager.integration;

import java.util.List;

public class ApacheHttpdVirtualhost {

	String listeningIPAddress="*";
	String listeningPort="80";
	String serverName;
	List<String> serverAliases;
	String proxyPath;
	
	
	public String toString(){
	
		String defaultConfig="<VirtualHost {listeningIPAddress}:{listeningPort}>";
		defaultConfig+="\tServerName {serverName}";
		defaultConfig+="\tServerAliases {serverAliases}";
		defaultConfig+="\tProxyPass {proxyPath}";
		defaultConfig+="\tProxyPassReverse {proxyPath}";
		defaultConfig+="\tErrorLog logs/{serverName}_error.log";
		defaultConfig+="\tCustomLog logs/{proxyPath}_access.log";
		defaultConfig += "</VirtualHost>";
		
		return defaultConfig;
		
	}


	public String getListeningIPAddress() {
		return listeningIPAddress;
	}


	public void setListeningIPAddress(String listeningIPAddress) {
		this.listeningIPAddress = listeningIPAddress;
	}


	public String getListeningPort() {
		return listeningPort;
	}


	public void setListeningPort(String listeningPort) {
		this.listeningPort = listeningPort;
	}


	public String getServerName() {
		return serverName;
	}


	public void setServerName(String serverName) {
		this.serverName = serverName;
	}


	public List<String> getServerAliases() {
		return serverAliases;
	}


	public void setServerAliases(List<String> serverAliases) {
		this.serverAliases = serverAliases;
	}


	public String getProxyPath() {
		return proxyPath;
	}


	public void setProxyPath(String proxyPath) {
		this.proxyPath = proxyPath;
	}
	
}
