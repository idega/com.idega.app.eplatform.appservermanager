package com.idega.eplatform.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * <p>
 * This class downloads a file from an URL and manages how much of it is downloaded.
 * </p>
 *  Last modified: $Date: 2005/06/09 14:55:35 $ by $Author: tryggvil $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.1 $
 */
public class FileDownloader implements Runnable{
	
	private URL url;
	private File downloadFile;
	private int bytesDownloaded;
	private int fileSizeInBytes;
	private boolean done=false;

	public FileDownloader(URL url,File downloadFile){
		setUrl(url);
		setDownloadFile(downloadFile);
	}

	public void run() {
		
		try {

			download();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void download() throws Exception {

		URLConnection connection = url.openConnection();
		connection.connect();
		setFileSizeInBytes(connection.getContentLength());
		
		InputStream input;
      	input = new BufferedInputStream(connection.getInputStream());

      	FileOutputStream out = new FileOutputStream(getDownloadFile());
      	int bufferlength=1024;
      	byte[] buffer = new byte[bufferlength];

      	int read = input.read(buffer);
      	while(read!=-1){
      		out.write(buffer,0,read);
      		setBytesDownloaded(getBytesDownloaded()+read);
      		read = input.read(buffer);
      	}

		out.close();
      	input.close();//close the file
		done=true;
	}

	public int getBytesDownloaded() {
		return bytesDownloaded;
	}

	
	public void setBytesDownloaded(int bytesDownloaded) {
		this.bytesDownloaded = bytesDownloaded;
	}

	
	public File getDownloadFile() {
		return downloadFile;
	}

	
	public void setDownloadFile(File downloadFile) {
		this.downloadFile = downloadFile;
	}

	
	public int getFileSizeInBytes() {
		return fileSizeInBytes;
	}

	
	public void setFileSizeInBytes(int fileSizeInBytes) {
		this.fileSizeInBytes = fileSizeInBytes;
	}

	/**
	 * <p>
	 * Return the percent completed in the range from 0..1 which represent 0-100%
	 * </p>
	 * @return
	 */
	public float getPercentCompleted() {
		//return percentCompleted;
		if(getFileSizeInBytes()!=0){
			return getBytesDownloaded()/getFileSizeInBytes();
		}
		return 0;
	}

	public boolean isDone(){
		return done;
	}
	
	public URL getUrl() {
		return url;
	}
	
	public void setUrl(URL url) {
		this.url = url;
	}
	
	public String getDownloadState(){
		return "Downloaded "+getBytesDownloaded()+" bytes of "+getFileSizeInBytes()+" to "+getDownloadFile().toString();
	}
	
	public static void main(String[] args){
		//This is a dummy test method:
		URL url;
		try {
			File dir = new File("/Applications/eclipse3.1M7/adminserver");
			if(!dir.exists()){
				dir.mkdir();
			}
			File downloadFile = new File("/Applications/eclipse3.1M7/adminserver/jakarta-tomcat-5.0.28.zip");
			url = new URL("http://apache.rhnet.is/dist/jakarta/tomcat-5/v5.0.28/bin/jakarta-tomcat-5.0.28.zip");
			FileDownloader grabber = new FileDownloader(url,downloadFile);
			
			Thread thread = new Thread(grabber);
			thread.start();
			while(!grabber.isDone()){
				System.out.println(grabber.getDownloadState());
				Thread.sleep(3000);
			}
			System.out.println(grabber.getDownloadState());
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
