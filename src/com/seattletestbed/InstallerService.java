/*
 * Copyright (C) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.seattletestbed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.util.Log;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.seattletestbed.R;
import com.seattletestbed.process.SeattleScriptProcess;
import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.ForegroundService;
import com.googlecode.android_scripting.NotificationIdFactory;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import javax.net.ssl.SSLSession;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLException;
/**
 * 
 * SeattleOnAndroid Installer Service
 * 
 * Loosely based on the Service found in the ScriptForAndroidTemplate project in SL4A
 * 
 * modified to allow embedded python interpreter and scripts in the APK
 * 
 * based off Anthony Prieur & Daniel Oppenheim work https://code.google.com/p/android-python27/
 *  
 */
public class InstallerService extends ForegroundService {

	private final static int NOTIFICATION_ID = NotificationIdFactory.create();
	
	private Logger installerLogger = null;

	private final IBinder mBinder;

	private InterpreterConfiguration mInterpreterConfiguration = null;
	private AndroidProxy mProxy;
	private Notification notification;
	// An instance of the service, used to determine whether it is running or not
	private static InstallerService instance = null;
	private static Context context = null;
		static {
			instance = null;
		}
	private File pythonBinary;
	private String packageName;
	private File fileDir;
	// Checks whether the service is running or not
	public static boolean isInstalling(){
		return instance!=null;
	}

	// Binder class
	public class LocalBinder extends Binder {
		public InstallerService getService() {
			return InstallerService.this;
		}
	}

	public InstallerService() {
		super(NOTIFICATION_ID);
		mBinder = new LocalBinder();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		InstallerService.context = getApplicationContext();
	}

	// Checks whether the installation was successful by inspecting the installInfo log file
	public boolean checkInstallationSuccess(){
		// Prefer installInfo.new to installInfo.old
		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/seattle/seattle_repy/installInfo.new");
		if(!f.exists()){
			f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/seattle/seattle_repy/installInfo.old");
			if(!f.exists())
				return false;
		}
		try {
			// Iterate through the file line by line, remembering the previous line
			BufferedReader r = new BufferedReader(new FileReader(f));
			String line, prevLine = null;
			while((line = r.readLine())!=null)
				prevLine = line;
			return (prevLine != null && prevLine.contains("seattle completed installation"));
		} catch (Exception e) {
			// Log exception
			Log.e(Common.LOG_TAG, Common.LOG_EXCEPTION_READING_INSTALL_INFO, e);
		}
		return false;
	}

	@Override
	public void onStart(Intent intent, final int startId) {
		super.onStart(intent, startId);
		
		// Set instance to self
		instance = this;
		// Set python binary located at /data/data/com.seattleonandroid/files/python/bin/python
		pythonBinary = new File(this.getFilesDir().getAbsolutePath() + "/python/bin/python");
		packageName = this.getPackageName();
		fileDir = this.getFilesDir();
		
		// Start the Logger used during Installation
		try {
			initalizeInstallerLogger();
		} catch (IOException e) {
		e.printStackTrace();
		installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_WRITING_LOG_FILE, e);
		}
		
		installerLogger.info(Common.LOG_INFO_INSTALLER_STARTED);
		// Set up notification icon
		String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		Intent callIntent = new Intent(this, ScriptActivity.class);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, callIntent, 0);

		notification.setLatestEventInfo(this, this.getString(R.string.srvc_install_name), this.getString(R.string.srvc_install_copy), contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		mNotificationManager.notify(NOTIFICATION_ID, notification);

		final ForegroundService s = this;
		final Intent fInt = intent;
		
		Thread t = new Thread()
		{
			public void run(){
				// Create seattle root folder
				File seattleFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/seattle/");
				if (seattleFolder.mkdirs())
					; // folder created
				else
					; // folder not created
				
				File archive = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/seattle.zip");
				archive.delete();
		
				//String user_hash = ReferralReceiver.retrieveReferralParams(s.getApplicationContext()).get("utm_content");
				//if (user_hash == null)
				//	user_hash = "flibble";
				
				String DownloadURLString = ReferralReceiver.retrieveReferralParams(s.getApplicationContext()).get("utm_source");
				if (DownloadURLString == null)
					DownloadURLString = Common.DEFAULT_DOWNLOAD_URL;

				try {
					//URL url = new URL("https://seattlegeni.cs.washington.edu/geni/download/"+user_hash+"/seattle_win.zip");
					URL url = new URL(DownloadURLString);
					installerLogger.info(Common.LOG_INFO_DOWNLOADING_FROM + url.toString() );

					URLConnection ucon;
					InputStream is;

					if (url.getProtocol().compareTo("https") == 0) {
						// https host
						//
						// REQUIRES REVIEW -- POSSIBLE SECURITY FLAW
						//
						// a host is trusted only if it is on the whitelist. the certificates and co. are not checked.
						//
						HostnameVerifier oldHV = HttpsURLConnection.getDefaultHostnameVerifier();
						HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() { 
									public boolean verify(String hostname, SSLSession session) { 
								installerLogger.info(hostname);
								for (String trustedHostname : Common.TRUSTED_DOWNLOAD_HOSTNAMES_WHITELIST) {
									if (trustedHostname.compareTo(hostname) == 0) {
										// Host on whitelist --> trust
										installerLogger.info(Common.LOG_INFO_UNTRUSTED_HOST_CHECK_WHITELIST_OK );
										return true;
									}
								};
								return false;
	            				}});
						SSLContext sslContext = SSLContext.getInstance("TLS");
						sslContext.init(null, new X509TrustManager[] {
							new X509TrustManager(){ 
								public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException, IllegalArgumentException {} 
								public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException, IllegalArgumentException {} 
								public X509Certificate[] getAcceptedIssuers() { 
									return new X509Certificate[0];
								}
							}
						}, new SecureRandom());
						SSLSocketFactory oldSSF = HttpsURLConnection.getDefaultSSLSocketFactory();
						HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
						ucon = url.openConnection();
						is = ucon.getInputStream();
						HttpsURLConnection.setDefaultHostnameVerifier(oldHV);
						HttpsURLConnection.setDefaultSSLSocketFactory(oldSSF);
					} else {
						// non-https host
						// the host is trusted in case its hostname is on the whitelist
						boolean trusted = false;
						for (String trustedHostname : Common.TRUSTED_DOWNLOAD_HOSTNAMES_WHITELIST) {
							if (trustedHostname.compareTo(url.getHost()) == 0) {
								// Host on whitelist --> trust
								trusted = true;
							}
						};
						if (!trusted) {
							// hostname not on whitelist, abort installation
							installerLogger.severe(Common.LOG_EXCEPTION_UNTRUSTED_HOST);
							throw new Exception("Untrusted host.");
						}
						installerLogger.info(Common.LOG_INFO_UNTRUSTED_HOST_CHECK_WHITELIST_OK);
						ucon = url.openConnection();
						is = ucon.getInputStream();
					}
					//InputStream is = ucon.getInputStream();
					BufferedInputStream bis = new BufferedInputStream(is);
					FileOutputStream fos = new FileOutputStream(archive);
					int len = 0;
					byte[] buffer = new byte[4096];
					installerLogger.info(Common.LOG_INFO_DOWNLOAD_STARTED);
					while ((len = bis.read(buffer)) != -1) {
						fos.write(buffer,0,len);
					}
					installerLogger.info(Common.LOG_INFO_DOWNLOAD_FINISHED);
					bis.close();
					fos.close();
				} catch (SSLException e) {
					installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_UNTRUSTED_HOST, e);
					instance = null;
					ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
					// Stop service
					stopSelf(startId);
					return;
				} catch (MalformedURLException e) {
					installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_MALFORMED_URL, e);
					instance = null;
					ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
					// Stop service
					stopSelf(startId);
					return;
				} catch (UnknownHostException e) {
					installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_COULD_NOT_RESOLVE_HOST, e);
					instance = null;
					ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
					// Stop service
					stopSelf(startId);
					return;
				} catch (IOException e) {
					installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_DOWNLOAD_ERROR, e);
					instance = null;
					ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
					// Stop service
					stopSelf(startId);
					return;					
				} catch (Exception e) {
					installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_DOWNLOAD_UNKNOWN_ERROR, e);
					instance = null;
					ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
					// Stop service
					stopSelf(startId);
					return;					
				}
		
				// Unzip archive
				try{
					FileInputStream fis = new FileInputStream(archive); 
					Utils.unzip(fis, Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/", false);
				} catch (Exception e) {
					installerLogger.log(Level.SEVERE, Common.LOG_EXCEPTION_UNZIPPING, e);
					archive.delete();				
					instance = null;
					ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
					// Stop service
					stopSelf(startId);
					return;					
				}
				installerLogger.info(Common.LOG_INFO_UNZIP_COMPLETED);
				// Remove archive
				archive.delete();
		
				// Update notification
				notification.setLatestEventInfo(s, s.getString(R.string.srvc_install_name), s.getString(R.string.srvc_install_config), contentIntent);
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				mNotificationManager.notify(NOTIFICATION_ID, notification);
		
				// Get installer script file
				File installer = new File(Environment.getExternalStorageDirectory().getAbsolutePath() 
						+ "/sl4a/seattle/seattle_repy/seattleinstaller.py");
		
				// Get percentage of resources to donate 
				Bundle b = fInt.getExtras();
				int donate = b.getInt(ScriptActivity.RESOURCES_TO_DONATE, 50);
				
				String[] iflist = b.getStringArray(ScriptActivity.PERMITTED_INTERFACES);
				
				// Get information about cores and storage space
				StatFs statfs = new StatFs(Environment.getExternalStorageDirectory().getPath());
				int cores = Runtime.getRuntime().availableProcessors();
				int freeSpace = statfs.getAvailableBlocks() *statfs.getBlockSize();
				
				// Set environmental variables
				Map<String, String> env = new HashMap<String, String>();
				env.put("SEATTLE_AVAILABLE_CORES", Integer.toString(cores));
				env.put("SEATTLE_AVAILABLE_SPACE", Integer.toString(freeSpace));
		
				//set python 2.7 environmental variables to pass to interpreter
				env.put("PYTHONPATH",
						Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/"
								+ packageName + "/extras/python"
								+ ":"
								+ fileDir.getAbsolutePath()
								+ "/python/lib/python2.7/lib-dynload"
								+ ":"
								+ fileDir.getAbsolutePath()
								+ "/python/lib/python2.7");
				
				env.put("TEMP", Environment.getExternalStorageDirectory().getAbsolutePath()
						+ "/" + packageName + "/extras/tmp");
				
				env.put("PYTHONHOME", fileDir.getAbsolutePath() + "/python");
				
				env.put("LD_LIBRARY_PATH", fileDir.getAbsolutePath()
						+ "/python/lib"
						+ ":" 
						+ fileDir.getAbsolutePath() + "/python/lib/python2.7/lib-dynload");
				
				// Set arguments
				List<String> args = new ArrayList<String>();
				args.add(installer.toString());
				args.add("--percent");
				args.add(Integer.toString(donate)); // make sure that dot is used as the decimal separator instead of comma
				args.add("--disable-startup-script");
				args.add("True");
				
				if (iflist != null)
				{
					for(int i=0; i<iflist.length; i++)
					{
						args.add("--nm-iface");
						args.add(iflist[i]);
						args.add("--repy-iface");
						args.add(iflist[i]);
					}
					args.add("--repy-nootherips");
				}
				
				String optionalArgs = b.getString(ScriptActivity.OPTIONAL_ARGUMENTS);
				if (optionalArgs != null)
					args.add(optionalArgs);
				installerLogger.info(Common.LOG_INFO_STARTING_INSTALLER_SCRIPT);
				
				mProxy = new AndroidProxy(s, null, true);
				mProxy.startLocal();
				//mLatch.countDown();
				// Launch installer
				SeattleScriptProcess.launchScript(installer, mInterpreterConfiguration, mProxy, new Runnable() {
					@Override
					public void run() {
						mProxy.shutdown();
		
						// Mark installation terminated
						instance = null;

						installerLogger.info(Common.LOG_INFO_TERMINATED_INSTALLER_SCRIPT);
						// Check whether the installation was successful or not
						if(checkInstallationSuccess())
						{
							// Send message to activity about success
							ScriptActivity.handler.sendEmptyMessage(ScriptActivity.SEATTLE_INSTALLED);
						}
						else
						{
							// If it was unsuccessful, remove nmmain.py, so that the app will not think seattle is installed
							// Other files are not removed to preserve the log files 
							FileUtils.delete(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/seattle/seattle_repy/nmmain.py"));
							// Send message to activity about failure
							instance = null;
							ScriptActivity.handler.sendEmptyMessage(ScriptActivity.INSTALL_FAILED);
						}
		
						// Stop service
						stopSelf(startId);
					}
				}, installer.getParent(),Environment.getExternalStorageDirectory().getAbsolutePath() 
				+ "/" + packageName, args, env, pythonBinary);
			};
		};
		
		t.start();
	}
	private void initalizeInstallerLogger()throws IOException {
		// Make sure the InstallerLogger is only initalized once
		if (installerLogger != null) {
			return;
		}
		installerLogger = Logger.getLogger("SeattleOnAndroid");
		installerLogger.setLevel(Level.INFO);
		File logDir = new File(Environment.getExternalStorageDirectory()
	            +File.separator
	            +"sl4a" //folder name
	            +File.separator
	            +"seattle" //folder name
	            +File.separator
	            +"seattle_repy"); //folder name
	    // Check if directory exists, if not create it     
		if (!logDir.isDirectory()) {
			logDir.mkdirs();
			}
		// Check if SDCARD is mounted
	    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
	        //handle case of no SDCARD present
	    	return;
	    } else {
	    	// the %g is the number of the current log in the rotation
	        String logFile = logDir.getAbsolutePath() + File.separator + "installerDebug_%g.log";  
	        FileHandler logHandler = new FileHandler(logFile, 1024 * 1024 ,5);
	        logHandler.setFormatter(new SimpleFormatter());        
		    installerLogger.addHandler(logHandler);    
		}
	}
	
	// Create initial notification
	@Override
	protected Notification createNotification() {
		
		Notification notification = new Notification(R.drawable.seattlelogo, this.getString(R.string.srvc_install_start), System.currentTimeMillis());
		
		Intent intent = new Intent(this, ScriptActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		notification.setLatestEventInfo(this, this.getString(R.string.srvc_install_name), this.getString(R.string.srvc_install_start), contentIntent);
		
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		this.notification = notification;
		
		return notification;
	}
}
