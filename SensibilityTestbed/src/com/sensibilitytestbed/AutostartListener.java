package com.sensibilitytestbed;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
/***
 * 
 * Listens for a BOOT_COMPLETED intent and starts the application if AUTOSTART_ON_BOOT is set
 *
 */
public class AutostartListener extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		// Executed on successful booting
		new AsyncStart(context).execute();
	}
	public class AsyncStart extends AsyncTask<Void, Integer, String> {
	    private Context context;
	    public AsyncStart(Context context) {
	        super();
	        this.context = context;
	    }
	    @Override
	    protected String doInBackground(Void... params) {
	    	while (true) {		
	    		boolean isInstalled = (new File(ScriptActivity.getSeattlePath()+"seattle_repy/","nmmain.py")).exists();
	    		SharedPreferences settings = context.getSharedPreferences(ScriptActivity.SEATTLE_PREFERENCES, Context.MODE_WORLD_WRITEABLE);
	    		// Check if the app is installed and is to be run on startup
	    		if(isInstalled&&settings.getBoolean(ScriptActivity.AUTOSTART_ON_BOOT,true)) {
			    	  Intent serviceIntent = new Intent();
			    	  serviceIntent.setAction("com.sensibilitytestbed.ScriptService");
			    	  ScriptService.serviceInitiatedByUser = true;
			    	  // Start the service
					  Log.i(Common.LOG_TAG, Common.LOG_INFO_SEATTLE_STARTED_AUTOMATICALLY);
					  context.startService(new Intent(context.getApplicationContext(), ScriptService.class));
			    	  break;
				}
				else {
					try {
						if(!isInstalled)
							Log.i(Common.LOG_TAG, "Sensibility Testbed not properly installed -- Autostart attempt failed");
						else
							Log.i(Common.LOG_TAG, "Autostart on boot set to false");
						Thread.sleep(2000);
					} catch (InterruptedException e) {}
				}	
			}
	        return null;
	    }
	} 
}
