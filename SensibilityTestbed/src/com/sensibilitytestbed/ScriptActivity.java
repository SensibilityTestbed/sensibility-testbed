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

package com.sensibilitytestbed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.FileUtils;

/**
 * 
 * Loosely based on the ScriptActivity found in the ScriptForAndroidTemplate
 * package in SL4A
 * 
 * This class represents the main activity performed by the SeattleOnAndroid app
 * 
 */
public class ScriptActivity extends Activity {

	// Use int values instead of enums for easier message handling
	public final static int SEATTLE_INSTALLED = 14;
	public final static int INSTALL_FAILED = 15;

	// Names of the keys to use
	public final static String AUTOSTART_ON_BOOT = "autostart_on_boot";
	public final static String AUTOSTART_DELAY = "autostart_delay";
	public final static String SEATTLE_PREFERENCES = "seattlepreferences";
	public final static String RESOURCES_TO_DONATE = "resources_to_donate";
	public final static String PERMITTED_INTERFACES = "permitted_interfaces";
	public final static String OPTIONAL_ARGUMENTS = "optional_arguments";
	public final static String UPDATE_MESSAGE_ID = "UPD";
	// Constants used in calculating the percentage to donate
	public final static int MINIMUM_DONATE = 1;
	public final static int MAXIMUM_DONATE = 100;
	public final static int DEFAULT_DONATE = 20;
	public final static int MAXIMUM_SEEKBAR = MAXIMUM_DONATE - MINIMUM_DONATE
			+ 1;
	// Some private variables
	private int donate = -1;
	private int currentContentView;
	private File currentLogFile;
	private ArrayList<File> files;
	// this shows a progress indicator when unpacking python
	private ProgressDialog pythonProgress;
	// Workaround -- status toggle-button could be set incorrectly right after
	// installation
	private static boolean autostartedAfterInstallation = false;

	// This is initialized by the onStart() method.  It needs to be static
	// as it is referred to in a lot of different classes.  This should be fine
	// since a ScriptActivity instance will be the first thing instantiated (as
	// far as anything that uses seattleInstallDirectory is concerned).
	// This should NOT be modified after initialization.
	public static File seattleInstallDirectory;

	// Message handler used for notifying the activity
	public static MyMessageHandler handler;

	private SharedPreferences settings;

	// Returns the root directory of seattle
	// Not to be confused with seattle_repy directory, which is a subdirectory
	// of seattle-root
	public static String getSeattlePath() {
		return seattleInstallDirectory.getAbsolutePath()
				+ "/sl4a/seattle/";
	}

	// Check if seattle is installed by checking if nmmain.py exists
	private boolean isSeattleInstalled() {
		return (new File(getSeattlePath() + "seattle_repy/nmmain.py")).exists();
	}

	// setup python progressDialog
	private void preparePythonProgress() {
		this.pythonProgress = new ProgressDialog(this);
		pythonProgress.setMessage("Unpacking Python...");
		pythonProgress.setIndeterminate(true);
		pythonProgress.setCancelable(false);
		pythonProgress.show();
	}

	// Message handler class
	public class MyMessageHandler extends Handler {
		private ScriptActivity a;

		public MyMessageHandler(ScriptActivity a) {
			super();
			this.a = a;
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				// Installation finished -> refresh screen
				if (msg.what == SEATTLE_INSTALLED || msg.what == INSTALL_FAILED) {
					// Start Seattle automatically after installation
					if (msg.what == SEATTLE_INSTALLED) {
						ScriptActivity.autostartedAfterInstallation = true;
						ScriptService.serviceInitiatedByUser = true;
						startService(new Intent(getBaseContext(),
								ScriptService.class));
					}
					// If AUTOSTART_ON_BOOT key does not exist, create it
					// Default value: true
					if (!settings.contains(AUTOSTART_ON_BOOT)) {
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(AUTOSTART_ON_BOOT, true);
						editor.commit();
					}

					// Reload layout
					showFrontendLayout();

					// Show dialog with information about the installation
					// outcome
					String text;
					if (msg.what == SEATTLE_INSTALLED) {
						text = Common.LOG_INFO_INSTALL_SUCCESS;
						Log.i(Common.LOG_TAG, Common.LOG_INFO_INSTALL_SUCCESS);
					} else {
						text = Common.LOG_INFO_INSTALL_FAILURE;
						Log.i(Common.LOG_TAG, Common.LOG_INFO_INSTALL_FAILURE);
					}
					new AlertDialog.Builder(a)
							.setTitle(getString(R.string.app_name))
							.setMessage(text)
							.setNeutralButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				}
			} catch (Exception e) {
				// Log exceptions
				Log.e(Common.LOG_TAG, Common.LOG_EXCEPTION_MESSAGE_HANDLING, e);
			}
		}
	}

	// Prepares options menu
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		switch (currentContentView) {
		case R.layout.main:
			inflater.inflate(R.menu.main_menu, menu);
			return true;
		case R.layout.install:
		case R.layout.basic_install:
			inflater.inflate(R.menu.install_menu, menu);
			return true;
		case R.layout.logfileview:
		case R.layout.logmenuview:
			inflater.inflate(R.menu.log_menu, menu);
			return true;
		case R.layout.about:
			inflater.inflate(R.menu.about_menu, menu);
			return true;
		}
		return false;
	}

	// Displays the contents of the file line by line
	private void showLogFile(File file) {
		// Store reference to file, so that it can be found on refresh
		currentLogFile = file;
		this.setContentView(R.layout.logfileview);
		currentContentView = R.layout.logfileview;
		// Set up TextViews
		TextView twDesc = (TextView) this
				.findViewById(R.id.textViewLogDescription);
		twDesc.setText(file.getName() + ":");
		TextView twCont = (TextView) this
				.findViewById(R.id.textViewLogContents);
		// File does not exist
		if (!file.exists())
			twCont.setText("File does not exist!");
		else {
			// File exists, iterate through it
			try {
				BufferedReader r = new BufferedReader(new FileReader(file));
				String line;
				twCont.setText("");
				while ((line = r.readLine()) != null) {
					twCont.append(line + "\n");
				}
				r.close();
			} catch (Exception e) {
				// Log exception
				Log.e(Common.LOG_TAG, Common.LOG_EXCEPTION_READING_LOG_FILE, e);
			}
		}
		// Post event to scroll down to the bottom of the page
		final ScrollView sv = (ScrollView) this
				.findViewById(R.id.logFileScrollView);
		sv.post(new Runnable() {
			@Override
			public void run() {
				sv.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
		sv.scrollTo(0, sv.getBottom());
	}

	// Back button was pressed by the user
	@Override
	public void onBackPressed() {
		if (currentContentView == R.layout.logmenuview) {
			showFrontendLayout();
		} else if (currentContentView == R.layout.logfileview) {
			showAvailableLogListing();
		} else if (currentContentView == R.layout.about) {
			showFrontendLayout();
		} else {
			// Let android handle it.
			super.onBackPressed();
		}
	}

	// Get a listing of the (hopefully) seattle specific log files in the
	// directory
	public static ArrayList<File> getLogFilesInDirectory(File directory) {
		// Accepts only directories
		FileFilter directoryFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		};
		// Accepts files with .log extension or
		// (nodemanager|softwareupdater|installInfo).(old|new)
		FileFilter logFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				// No directories accepted by this filter
				if (pathname.isDirectory())
					return false;
				String filename = pathname.getName();
				return filename.endsWith(".log")
						|| ((filename.startsWith("nodemanager")
								|| filename.startsWith("softwareupdater") || filename
									.startsWith("installInfo")) && (filename
								.endsWith(".old") || filename.endsWith(".new")));
			}
		};

		// Iterate through subdirectories
		return getLogFilesInDirectoryPrivate(directory, directoryFilter,
				logFilter);
	}

	// Get listing of files in the directory tree conforming to some filters
	private static ArrayList<File> getLogFilesInDirectoryPrivate(File dir,
			FileFilter dirFilter, FileFilter logFilter) {
		ArrayList<File> result = new ArrayList<File>();
		if (!dir.exists())
			return result;
		// Get files in this directory
		File[] files = dir.listFiles(logFilter);
		if (files != null)
			result.addAll(Arrays.asList(files));
		// Get subdirectories
		File[] subdirs = dir.listFiles(dirFilter);
		// Iterate through subdirectories
		if (subdirs != null)
			for (int i = 0; i < subdirs.length; i++) {
				result.addAll(getLogFilesInDirectoryPrivate(subdirs[i],
						dirFilter, logFilter));
			}
		return result;
	}

	// Show a listing of the available log files
	private void showAvailableLogListing() {
		setContentView(R.layout.logmenuview);
		currentContentView = R.layout.logmenuview;
		ListView lv = (ListView) findViewById(R.id.listView1);
		// Get log files
		files = getLogFilesInDirectory(new File(getSeattlePath()));
		ArrayList<String> strings = new ArrayList<String>();
		final ScriptActivity instance = this;
		for (int i = 0; i < files.size(); i++) {
			strings.add(files.get(i).getName());
		}
		// Set up ListView
		lv.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item,
				strings.toArray(new String[strings.size()])));
		// Set up onClickListener
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				instance.showLogFile(files.get(arg2));
			}
		});
	}

	// Handle click events from options menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.view_log_button:
			// Show available log files
			showAvailableLogListing();
			return true;
		case R.id.uninstall_seattle_button:
			// Uninstall seattle
			final ScriptActivity sa = this;
			new AlertDialog.Builder(this)
					.setMessage(
							"Would you really like to uninstall Sensibility Testbed?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Log.i(Common.LOG_TAG,
											Common.LOG_INFO_UNINSTALL_INITIATED);
									// Kill service, in case it was running
									if (ScriptService.isServiceRunning())
										killService();
									// Remove Seattle folder
									FileUtils
											.delete(new File(getSeattlePath()));
									if (settings.contains(AUTOSTART_ON_BOOT)) {
										SharedPreferences.Editor editor = settings
												.edit();
										editor.remove(AUTOSTART_ON_BOOT);
										editor.commit();
									}
									new AlertDialog.Builder(sa)
											.setMessage(
													"Seattle uninstalled successfully!")
											.setNeutralButton(
													"Ok",
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
														}
													}).create().show();
									Log.i(Common.LOG_TAG,
											Common.LOG_INFO_UNINSTALL_SUCCESS);
									showBasicInstallLayout();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create().show();
			return true;
		case R.id.back:
			// Back button
			onBackPressed();
			return true;
		case R.id.refresh:
			// Refresh
			doRefresh();
			return true;
		case R.id.view_about_button:
			showAboutLayout();
			return true;
		default:
			return false;
		}
	}

	// Kills the ScriptService using startService, by sending a flagged intent
	private void killService() {
		Intent intent = new Intent(getBaseContext(), ScriptService.class);
		intent.putExtra("KILL_SERVICE", true);
		startService(intent);
	}

	// Refresh view
	private void doRefresh() {
		if (currentContentView == R.layout.logmenuview) {
			showAvailableLogListing();
		} else if (currentContentView == R.layout.logfileview) {
			showLogFile(currentLogFile);
		}
	}

	// Show the main (seattle already installed) layout
	private void showMainLayout() {
		setContentView(R.layout.main);
		currentContentView = R.layout.main;
		// Set up status toggle button
		final ToggleButton toggleStatus = (ToggleButton) findViewById(R.id.toggleStatus);
		toggleStatus.setChecked(ScriptService.isServiceRunning());

		if (ScriptActivity.autostartedAfterInstallation) {
			ScriptActivity.autostartedAfterInstallation = false;
			toggleStatus.setChecked(true);
		}

		toggleStatus.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// Start service
					ScriptService.serviceInitiatedByUser = true;
					startService(new Intent(getBaseContext(),
							ScriptService.class));
				} else {
					// Kill service
					killService();
				}
			}
		});
		// Set up autostart checkbox
		final CheckBox checkBoxAutostart = (CheckBox) findViewById(R.id.checkBoxAutostart);
		checkBoxAutostart
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// Store changes
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(AUTOSTART_ON_BOOT, isChecked);
						editor.commit();
					}
				});
		checkBoxAutostart.setChecked(settings.getBoolean(AUTOSTART_ON_BOOT,
				true));
	}

	// Show install layout
	private void showBasicInstallLayout() {
		setContentView(R.layout.basic_install);
		currentContentView = R.layout.basic_install;
		final Button buttonInstall = (Button) findViewById(R.id.basicinstallbutton);
		final Button buttonAdvanced = (Button) findViewById(R.id.showadvancedoptionsbutton);
		handler = new MyMessageHandler(this);

		String referrer = ReferralReceiver.retrieveReferralParams(
				getApplicationContext()).get("utm_content");
		if (referrer == null)
			referrer = "Altruistic donation";
		else
			referrer = "Donate to: " + referrer;
		final TextView referrerView = (TextView) findViewById(R.id.referrerview);
		referrerView.setText(referrer);

		donate = DEFAULT_DONATE;
		// Set up install button listener
		buttonInstall.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(getBaseContext(), InstallerService.class);
				i.putExtra(RESOURCES_TO_DONATE, donate);
				startService(i);
				showInstallingLayout();
			}
		});
		buttonAdvanced.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showAdvancedInstallLayout();
			}
		});
	}

	// Show install layout
	private void showAdvancedInstallLayout() {
		setContentView(R.layout.install);
		currentContentView = R.layout.install;

		final Button buttonBasic = (Button) findViewById(R.id.showbasicoptionsbutton);
		buttonBasic.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showBasicInstallLayout();
			}
		});

		final Button button = (Button) findViewById(R.id.buttonInstall);
		handler = new MyMessageHandler(this);
		final TextView tw = (TextView) findViewById(R.id.textView6);
		final String donateString = this.getString(R.string.resource_donate);
		// Set up donation-percentage seekbar
		final SeekBar sb = (SeekBar) findViewById(R.id.seekBar1);
		sb.setMax(MAXIMUM_SEEKBAR);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// Update on changes to the seekbar
				donate = progress + MINIMUM_DONATE;
				if (donate > MAXIMUM_DONATE)
					donate = MAXIMUM_DONATE;
				tw.setText(donateString + ": " + Integer.toString(donate));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		String referrer = ReferralReceiver.retrieveReferralParams(
				getApplicationContext()).get("utm_content");
		if (referrer == null)
			referrer = "Altruistic donation";
		else
			referrer = "Donate to: " + referrer;

		final TextView referrerView = (TextView) findViewById(R.id.referview);
		referrerView.setText(referrer);

		String DownloadURLString = ReferralReceiver.retrieveReferralParams(
				getApplicationContext()).get("utm_source");
		if (DownloadURLString == null)
			DownloadURLString = Common.DEFAULT_DOWNLOAD_URL;
		final TextView downloadView = (TextView) findViewById(R.id.downloadview);
		downloadView.setText("URL: " + DownloadURLString);

		// If donation was not yet set, default to DEFAULT_DONATE
		if (donate == -1)
			donate = DEFAULT_DONATE;
		tw.setText(donateString + ": " + Integer.toString(donate));
		sb.setProgress(donate - MINIMUM_DONATE);

		final EditText permittedInterfaces = (EditText) findViewById(R.id.permittedInterfaces);
		final TextView permView = (TextView) findViewById(R.id.permifView);
		final CheckBox allBox = (CheckBox) findViewById(R.id.checkBox1);
		allBox.setChecked(true);
		permittedInterfaces.setVisibility(View.INVISIBLE);
		permView.setVisibility(View.INVISIBLE);

		final EditText optionalArgs = (EditText) findViewById(R.id.optionalEdit);

		allBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					permittedInterfaces.setVisibility(View.INVISIBLE);
					permView.setVisibility(View.INVISIBLE);
				} else {
					permittedInterfaces.setVisibility(View.VISIBLE);
					permView.setVisibility(View.VISIBLE);
				}
			}

		});

		String iflist = null;

		Enumeration<NetworkInterface> networkInterfaces;
		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) networkInterfaces
						.nextElement();
				if (iflist == null)
					iflist = networkInterface.getName();
				else
					iflist += "," + networkInterface.getName();
			}
		} catch (SocketException e) {
			iflist = null;
			Log.e(Common.LOG_TAG, Common.LOG_EXCEPTION_GETTING_IFS, e);
		}

		permittedInterfaces.setText(iflist);

		// Set up install button listener
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final Intent i = new Intent(getBaseContext(),
						InstallerService.class);
				i.putExtra(RESOURCES_TO_DONATE, donate);
				String permlist[] = null;
				if (!allBox.isChecked()) {
					String intlist = permittedInterfaces.getText().toString();
					if (intlist.trim().compareTo("") != 0)
						permlist = intlist.split(",");
					i.putExtra(PERMITTED_INTERFACES, permlist);
				}
				i.putExtra(OPTIONAL_ARGUMENTS, optionalArgs.getText()
						.toString());

				startService(i);

				showInstallingLayout();
			}
		});
	}

	// Show the most appropriate layout
	private void showFrontendLayout() {
		if (InstallerService.isInstalling()) {
			// Installer still running
			showInstallingLayout();
		} else if (isSeattleInstalled()) {
			// Already installed
			showMainLayout();
		} else {
			// Not yet installed
			showBasicInstallLayout();
		}
	}

	// Show installation in progress layout
	private void showInstallingLayout() {
		setContentView(R.layout.installing);
		currentContentView = R.layout.installing;
	}

	// Show external storage device not mounted layout
	private void showNotMountedLayout() {
		setContentView(R.layout.notmounted);
		currentContentView = R.layout.notmounted;
	}

	// Show about box layout
	private void showAboutLayout() {
		setContentView(R.layout.about);

		TextView tw = (TextView) findViewById(R.id.aboutVersionView);
		try {
			// Get version name
			tw.setText("v"
					+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			// This exception should not occur
			Log.e(Common.LOG_TAG, Common.LOG_EXCEPTION_ABOUT_NNF, e);
		}
		currentContentView = R.layout.about;
	}

	// Unpack and set file permission for python files located in res/raw based
	// off of
	// Anthony Prieur & Daniel Oppenheim work
	// https://code.google.com/p/android-python27/
	private void copyPythonToLocal() {
		String zipPath, zipName;
		InputStream content;
		R.raw a = new R.raw();
		java.lang.reflect.Field[] t = R.raw.class.getFields();
		Resources resources = getResources();
		Log.i(Common.LOG_TAG, Common.LOG_INFO_PYTHON_UNZIP_STARTED);
		for (int i = 0; i < t.length; i++) {
			try {
				// Get path of zip to unpack
				zipPath = resources.getText(t[i].getInt(a)).toString();
				// Extract the zipName from zipPath
				zipName = zipPath.substring(zipPath.lastIndexOf('/') + 1,
						zipPath.length());
				content = getResources().openRawResource(t[i].getInt(a));
				content.reset();

				// Python_27 we unpack to ->
				// /data/data/com.sensibilitytestbed/files/python
				if (zipName.endsWith(Common.PYTHON_ZIP_NAME)) {
					// This is the Python binary. It needs to live in /data/data/...., 
					// as it can't be made executable on the SDcard due to VFAT.
					// Thus, we must not use seattleInstallDirectory for this.
					Utils.unzip(content, this.getFilesDir().getAbsolutePath() + "/", true);
					// set file permissions
					FileUtils.chmod(new File(this.getFilesDir().getAbsolutePath() + "/python/bin/python"), 0755);
				}
				// Python_extras_27 we unpack to ->
				// /sdcard/com.sensibilitytestbed/extras/python
				else if (zipName.endsWith(Common.PYTHON_EXTRAS_ZIP_NAME)) {
					Utils.unzip(content, seattleInstallDirectory.getAbsolutePath() +
							"/extras/", true);
				}
			} catch (Exception e) {
				Log.e(Common.LOG_TAG, Common.LOG_EXCEPTION_PYTHON_UNZIPPING, e);
			}
		}
		Log.i(Common.LOG_TAG, Common.LOG_INFO_PYTHON_UNZIP_COMPLETED);
	}
	

	/*// check if an app is installed, thanks to 
	// http://www.grokkingandroid.com/checking-intent-availability/
	public static boolean isMyServiceInstalled(Context ctx, Intent intent) {
		final PackageManager mgr = ctx.getPackageManager();
		List<ResolveInfo> list = mgr.queryIntentActivities(intent, 
				            PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}*/
	
	// check if sl4a is running thanks to: 
	// http://stackoverflow.com/questions/7440473/android-how-to-check-if-the-intent-service-is-still-running-or-has-stopped-runni
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("com.googlecode.android_scripting.activity.ScriptingLayerService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	
	// Executed after the activity is started / resumed
	@Override
	protected void onStart() {
		super.onStart();
		// Load settings
		settings = getSharedPreferences(SEATTLE_PREFERENCES,
				MODE_WORLD_WRITEABLE);
		seattleInstallDirectory = getExternalFilesDir(null);
		Log.v(Common.LOG_TAG, "Application files will be placed in: " +
			seattleInstallDirectory.getAbsolutePath());

		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// External storage device not mounted
			showNotMountedLayout();
		} else {
			// Let's try SL4A!
			// Start SL4A
			// XXX Repeat of code in ScriptService.java!
			Log.v(Common.LOG_TAG, "Creating SL4A Intent....");
			Intent sl4aIntent = new Intent();
			// Thank you,
			// http://stackoverflow.com/questions/6829187/android-explicit-intent-with-target-component
			// and
			// http://stackoverflow.com/questions/2780102/open-another-application-from-your-own-intent/
			sl4aIntent
					.setComponent(ComponentName
							.unflattenFromString("com.googlecode.android_scripting/.activity.ScriptingLayerServiceLauncher"));
			sl4aIntent.setAction(Constants.ACTION_LAUNCH_SERVER);
			sl4aIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// XXX How good an idea is hardcoding the listen port?
			sl4aIntent.putExtra(Constants.EXTRA_USE_SERVICE_PORT, 45678);		
			
			if(!Utils.isMyServiceInstalled(getBaseContext(), sl4aIntent)) {

				Log.i(Common.LOG_TAG, "SL4A is not installed. I'll go install it now.");
				// Install SL4A
				// Since the required APK is included in res/raw, just announce
				// an intent to have it installed.
				// Loosely based on
				// http://stackoverflow.com/questions/11727237/programatically-install-apk-file-located-in-res-raw
				// Thank you!
				try {
					Log.i(Common.LOG_TAG, "Trying to install SL4A...");
					AssetManager assetManager = getAssets();
					InputStream in = null;
					OutputStream out = null;
					try {
						Log.v(Common.LOG_TAG, "Starting file copying...");
						String baseFileName = "sl4a_r6.apk";
						String assetFileName = "raw/" + baseFileName;
						String savedFileName = ScriptActivity.seattleInstallDirectory + 
								"/" + baseFileName;
						in = assetManager.open(assetFileName);
						out = new FileOutputStream(savedFileName);
						byte[] buffer = new byte[1024];
						int read;
						while ((read = in.read(buffer)) != -1) {
							out.write(buffer, 0, read);
						}
						Log.v(Common.LOG_TAG, "File should be in dir now: "
								+ savedFileName);
						in.close();
						in = null;
						out.flush();
						out.close();
						out = null;
						Log.v(Common.LOG_TAG,
								"About to voice SL4A install intent...");
						Intent sl4aInstallIntent = new Intent(
								Intent.ACTION_VIEW);
						sl4aInstallIntent.setDataAndType(
								Uri.fromFile(new File(savedFileName)),
								"application/vnd.android.package-archive");
						sl4aInstallIntent
								.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(sl4aInstallIntent);
						Log.v(Common.LOG_TAG,
								"Raised SL4A install intent, let's see what happens...");
						
					} catch (Exception e2) {
						// deal with copying problem
						Log.e(Common.LOG_TAG,
								"SL4A install failed with " + e2.toString());
					}

				} catch (Exception e3) {
					Log.e(Common.LOG_TAG,
							"I'm sorry, installing the SL4A apk from assets/raw failed. Error: "
									+ e3.toString());
				} // Done with SL4A install
			}
			else{
				// sl4a is installed. now check if it is running
				if (!isMyServiceRunning()){
					Log.i(Common.LOG_TAG, "SL4A has not yet started!!");
					
					try {
						startActivity(sl4aIntent); // NOT startService! D'oh!
						Log.i(Common.LOG_TAG, "SL4A started, yay!!");
					} catch (Exception e) {
						Log.e(Common.LOG_TAG, "Trying to start SL4A failed. Original error: "
										+ e.toString());			
					}
				}
				else{
					Log.i(Common.LOG_TAG, "SL4A has started already!!");
				}
			}
			


			File pythonBinary = new File(this.getFilesDir().getAbsolutePath() + 
					"/python/bin/python");
			// Check if python is installed
			if (!pythonBinary.exists()) {
				Log.e(Common.LOG_TAG,
						Common.LOG_EXCEPTION_NO_PYTHON_INTERPRETER);
				// Setup dialog to display when python unpacking is complete
				final Builder pythonComplete = new AlertDialog.Builder(this)
						.setMessage("Python unpack complete!")
						.setNeutralButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
									}
								});
				// Setup and display python unpacking progress dialog
				preparePythonProgress();
				new Thread(new Runnable() {
					@Override
					public void run() {
						// Python binary was not found -> install python
						copyPythonToLocal();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// Python unpacking finished so kill the
								// displayed progress bar
								pythonProgress.dismiss();
								// Show dialogue confirming python unpacking
								// success
								pythonComplete.create().show();
							}
						});
					}
				}).start();
			}
			// Python installation found

			// Python and SL4A installer -> Show main layout
			showFrontendLayout();
		}
	}

	// Executed after the activity is created, calls onStart()
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.onStart();
	}
}
