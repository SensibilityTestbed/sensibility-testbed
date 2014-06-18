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
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.seattletestbed.R;
import com.seattletestbed.process.SeattleScriptProcess;
import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.ForegroundService;
import com.googlecode.android_scripting.NotificationIdFactory;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * SeattleOnAndroid Nodemanager and Softwareupdater Service
 * 
 * Loosely based on the Service found in the ScriptForAndroidTemplate package in SL4A
 * 
 * modified to allow embedded python interpreter and scripts in the APK
 * 
 * based off Anthony Prieur & Daniel Oppenheim work https://code.google.com/p/android-python27/
 * 
 */
public class ScriptService extends ForegroundService {
	private final static int NOTIFICATION_ID = NotificationIdFactory.create();
	private final IBinder mBinder;

	// booleans used in shutting down the service
	private boolean killMe, isRestarting;

	// updater and nodemanager processes
	private SeattleScriptProcess updaterProcess;
	private SeattleScriptProcess seattlemainProcess;

	private InterpreterConfiguration mInterpreterConfiguration = null;
	private AndroidProxy mProxy;

	// workaround to make sure the service does not get restarted
	// when the system kills this service
	public static boolean serviceInitiatedByUser = false;

	// an instance of this service, used in determining 
	// whether the service is running or not
	private static ScriptService instance = null;

	// checks whether the service is running or not
	public static boolean isServiceRunning() {
		return instance != null;
	}

	// binder class
	public class LocalBinder extends Binder {
		public ScriptService getService() {
			return ScriptService.this;
		}
	}

	// on destroy
	@Override
	public void onDestroy() {
		Log.i(Common.LOG_TAG, Common.LOG_INFO_MONITOR_SERVICE_SHUTDOWN);
	}

	// set up notification and binder
	public ScriptService() {
		super(NOTIFICATION_ID);
		mBinder = new LocalBinder();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// on creation
	// checks, whether the start was initiated by the user or someone else
	@Override
	public void onCreate() {
		super.onCreate();
		if(!serviceInitiatedByUser){
			this.stopSelf();
		}
	}

	// Starts the updater process
	private void startUpdater() {
		
		if(updaterProcess != null && updaterProcess.isAlive()){
			// updater already up and running
			return;
		}
		
		Log.i(Common.LOG_TAG, Common.LOG_INFO_STARTING_SEATTLE_UPDATER);

		// Get updater script file
		File updater = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/sl4a/seattle/seattle_repy/softwareupdater.py");
		
		List<String> args = new ArrayList<String>();
		args.add(updater.toString()); //script to run
		
		//set python Binary
		File pythonBinary = new File(this.getFilesDir().getAbsolutePath() + "/python/bin/python");

		mProxy = new AndroidProxy(this, null, true);
		mProxy.startLocal();

		// Set environmental variables (softwareupdater uses them instead of command-line arguments)
		Map<String, String> env = new HashMap<String, String>();
		env.put("SEATTLE_RUN_NODEMANAGER_IN_FOREGROUND", "True");
		env.put("SEATTLE_RUN_SOFTWAREUPDATER_IN_FOREGROUND", "True");

		//2.7 set python environmental variables
		env.put("PYTHONPATH", 
				Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
				+ this.getPackageName() + "/extras/python"
				+ ":"
				+ this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7/lib-dynload"
				+ ":"
				+ this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7");
		
		env.put("TEMP", Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + this.getPackageName() + "/extras/tmp");
		
		env.put("PYTHONHOME", this.getFilesDir().getAbsolutePath() + "/python");
		
		env.put("LD_LIBRARY_PATH", this.getFilesDir().getAbsolutePath()
				+ "/python/lib" + ":" + this.getFilesDir().getAbsolutePath()
				+ "/python/lib/python2.7/lib-dynload");

		// Start script
		updaterProcess = SeattleScriptProcess.launchScript(updater, mInterpreterConfiguration, mProxy, new Runnable() {
			@Override
			public void run() {
				Log.i(Common.LOG_TAG, Common.LOG_INFO_SEATTLE_UPDATER_SHUTDOWN);
				mProxy.shutdown();
				if(!killMe) {
					// Exit was not initiated by the user
					if(updaterProcess.getReturnValue() == 200 || updaterProcess.getReturnValue() == 201)
						isRestarting = true; // Exited to be restarted

					if(!isRestarting) {
						// seattle stopped because of an unknown reason
						// -> restart immediately
						startUpdater();
					} else {
						if(updaterProcess.getReturnValue() == 200) {
							Log.i(Common.LOG_TAG, Common.LOG_INFO_RESTARTING_SEATTLE_MAIN_AND_UPDATER);
							seattlemainProcess.kill(); // Restart nodemanager and updater
							startSeattleMain();
							startUpdater();
						} else {
							Log.i(Common.LOG_TAG, Common.LOG_INFO_RESTARTING_SEATTLE_UPDATER);
							startUpdater(); // Restart updater only
						}
					}
				}
			}
		}, updater.getParent(), Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/" + this.getPackageName(), args, env, pythonBinary);
	}

	// Starts the nodemanager process
	private void startSeattleMain() {
		
		if(seattlemainProcess != null && seattlemainProcess.isAlive()){
			// nodemanager already up and running
			return;
		}
		
		Log.i(Common.LOG_TAG, Common.LOG_INFO_STARTING_SEATTLE_MAIN);
		// Get nodemanager script file
		File seattlemain = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sl4a/seattle/seattle_repy/nmmain.py");

		// Set arguments
		List<String> args = new ArrayList<String>();
		args.add(seattlemain.toString()); //name of script to run
		args.add("--foreground");

		File pythonBinary = new File(this.getFilesDir().getAbsolutePath() + "/python/bin/python");
		
		//env variables
		Map<String, String> environmentVariables = null;	
		environmentVariables = new HashMap<String, String>();
		
		// set python 2.7 environmental variables to pass to interpreter
		environmentVariables.put("PYTHONPATH",
				Environment.getExternalStorageDirectory().getAbsolutePath() + "/" 
						+ this.getPackageName() + "/extras/python" 
						+ ":"
						+ this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7/lib-dynload" 
						+ ":"
						+ this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7");
		
		environmentVariables.put("TEMP",
				Environment.getExternalStorageDirectory().getAbsolutePath()
						+ "/" + this.getPackageName() + "/extras/tmp");
		
		environmentVariables.put("PYTHONHOME", this.getFilesDir()
				.getAbsolutePath() + "/python");
		
		environmentVariables.put("LD_LIBRARY_PATH", this.getFilesDir().getAbsolutePath()
				+ "/python/lib"
				+ ":"
				+ this.getFilesDir().getAbsolutePath() + "/python/lib/python2.7/lib-dynload");

		mProxy = new AndroidProxy(this, null, true);
		mProxy.startLocal();

		// Start process
		seattlemainProcess = SeattleScriptProcess.launchScript(seattlemain, mInterpreterConfiguration, mProxy, new Runnable() {
			@Override
			public void run() {
				Log.i(Common.LOG_TAG, Common.LOG_INFO_SEATTLE_MAIN_SHUTDOWN);
				mProxy.shutdown();
				if(!isRestarting && !killMe) {
					// seattle stopped because of an unknown reason
					// -> restart immediately
					startSeattleMain();
				}
			}
		}, seattlemain.getParent(), Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/" + this.getPackageName(), args, environmentVariables, pythonBinary);
	}

	private void killProcesses(){
		// Set kill flag, stop processes
		Log.i(Common.LOG_TAG, Common.LOG_INFO_KILLING_SCRIPTS);
		killMe = true;
		instance = null;

		if(updaterProcess != null){
			updaterProcess.kill();
		}
		if(seattlemainProcess != null){
			seattlemainProcess.kill();
		}
	}
	// executed after each startService() call
	@Override
	public void onStart(Intent intent, final int startId) {
		Log.i(Common.LOG_TAG, Common.LOG_INFO_MONITOR_SERVICE_STARTED);
		Bundle b = intent.getExtras();
		if(b != null && b.getBoolean("KILL_SERVICE") ||
				!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			// Media not mounted correctly, or service is set to be killed
			killProcesses();
			stopSelf();
			return;
		}
		super.onStart(intent, startId);
		instance = this;

		// Init flags
		killMe = false;
		isRestarting = false;

		// Start Seattle
		startSeattleMain();
		startUpdater();
	}

	// Create notification icon
	@Override
	protected Notification createNotification() {
		Notification notification = new Notification(R.drawable.seattlelogo, this.getString(R.string.loading), System.currentTimeMillis());
		
		// set OnClick intent
		Intent intent = new Intent(this, ScriptActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		notification.setLatestEventInfo(this, this.getString(R.string.app_name), this.getString(R.string.loading), contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		return notification;
	}
}
