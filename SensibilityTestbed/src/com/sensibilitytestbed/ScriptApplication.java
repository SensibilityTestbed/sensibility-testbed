package com.sensibilitytestbed;

import com.googlecode.android_scripting.BaseApplication;
import android.content.Context;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration.ConfigurationObserver;

/**
 * 
 * BaseApplication for the SeattleOnAndroid process
 * 
 * Based on the BaseApplication found in the ScriptForAndroidTemplate package of SL4A
 *
 * Discovers the interpreter configurations to be used
 *
 */
public class ScriptApplication extends BaseApplication implements ConfigurationObserver {

	private static Context context = null;
	private static String thePackageName = null;
	private static String theFilesDir = null;
	
		public static Context getAppContext() {
			return ScriptApplication.context;
		}

		public static String getThePackageName() {
			return thePackageName;
		}
		
	  public static String getTheFilesDir() {
			return theFilesDir;
		}

	@Override
	public void onCreate() {
		
		// dirty way to access some needed info in GlobalConstants
		ScriptApplication.context = getApplicationContext();
		ScriptApplication.theFilesDir = this.getFilesDir().getAbsolutePath();
		ScriptApplication.thePackageName = this.getPackageName();
	}

	@Override
	public void onConfigurationChanged() {
	//this fn is empty because its an inherited abstract method and we dont use it 
	}
}
