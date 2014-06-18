package com.sensibilitytestbed;



// Contains common constants 
public final class Common {
	// Default download URL for seattle archive
	public static final String DEFAULT_DOWNLOAD_URL = "https://sensibilityclearinghouse.poly.edu/geni/download/altruistic/seattle_android.zip"; 
	// This one has the stdevel keys inside: "https://betaseattleclearinghouse.poly.edu/custom_install/5c311a0c016a6f216eac4ca4763b773ee2322035/installers/android/";

	// Trusted hostnames
	public static final String[] TRUSTED_DOWNLOAD_HOSTNAMES_WHITELIST = {
		"seattleclearinghouse.poly.edu", "betaseattleclearinghouse.poly.edu", "sensibilityclearinghouse.poly.edu",
		"blackbox.poly.edu", "custombuilder.poly.edu"
	};

	//Embedded Python Interpreter Zip Names	
	//2.7
	public static final String PYTHON_ZIP_NAME = "python_27.zip";
	public static final String PYTHON_EXTRAS_ZIP_NAME = "python_extras_27.zip";
	// Log -- Tag
	public static final String LOG_TAG = "SensibilityTestbed.com";

	// Log -- Exceptions
	public static final String LOG_EXCEPTION_MESSAGE_HANDLING = "Exception occured while handling message";
	public static final String LOG_EXCEPTION_READING_LOG_FILE = "Exception occured while reading log files";
	public static final String LOG_EXCEPTION_WRITING_LOG_FILE = "Exception occured while creating log files";
	public static final String LOG_EXCEPTION_GETTING_IFS = "Exception occured while getting interface names";
	public static final String LOG_EXCEPTION_ABOUT_NNF = "NameNotFound exception while displaying about box";
	public static final String LOG_EXCEPTION_NO_PYTHON_INTERPRETER = "No suitable interpreter for python was found";
	public static final String LOG_EXCEPTION_READING_INSTALL_INFO = "Exception occured while reading installinfo";
	public static final String LOG_EXCEPTION_MALFORMED_URL = "Malformed download url";
	public static final String LOG_EXCEPTION_COULD_NOT_RESOLVE_HOST = "Could not resolve download url host (check network connection)";
	public static final String LOG_EXCEPTION_DOWNLOAD_ERROR = "I/O or networking exception occured while downloading packages";
	public static final String LOG_EXCEPTION_EXCEPTION_UNZIPPING = "Exception occured while extracting packages";
	public static final String LOG_EXCEPTION_PYTHON_UNZIPPING = "Exception occured while extracting python files to local";
	public static final String LOG_EXCEPTION_UNZIPPING = "Exception occured while extracting packages";
	public static final String LOG_EXCEPTION_UNKNOWN_APPLICATION = "Unknown exception occured in ScriptApplication";
	public static final String LOG_EXCEPTION_UNZIPPING_FILE = "Exception occured while extracting archive: ";
	public static final String LOG_EXCEPTION_UNZIPPING_ARCHIVE = "Exception occured while extracting file: ";
	public static final String LOG_EXCEPTION_DOWNLOAD_UNKNOWN_ERROR = "Unknown exception occured during or before download";
	public static final String LOG_EXCEPTION_UNTRUSTED_HOST = "Untrusted host (host not on whitelist)";

	// Log -- Informative
	public static final String LOG_INFO_SEATTLE_STARTED_AUTOMATICALLY = "Sensibility Testbed started automatically";
	public static final String LOG_INFO_DOWNLOADING_PYTHON = "Python interpreter download initiated by user";
	public static final String LOG_INFO_INSTALLER_STARTED = "Installer service started";
	public static final String LOG_INFO_DOWNLOADING_FROM = "Downloading Sensibility Testbed from: ";
	public static final String LOG_INFO_DOWNLOAD_STARTED = "Download started"; 
	public static final String LOG_INFO_DOWNLOAD_FINISHED = "Download finished";
	public static final String LOG_INFO_UNZIP_COMPLETED = "Sensibility Testbed extracted successfully";
	public static final String LOG_INFO_PYTHON_UNZIP_STARTED = "Python extraction started";
	public static final String LOG_INFO_PYTHON_UNZIP_COMPLETED = "Python extracted successfully";
	public static final String LOG_INFO_STARTING_INSTALLER_SCRIPT = "Starting installer script";
	public static final String LOG_INFO_TERMINATED_INSTALLER_SCRIPT = "Installer script finished (not sure if successful)";
	public static final String LOG_INFO_STORED_REFERRAL_PARAMS = "Stored referral parameters: ";
	public static final String LOG_INFO_MONITOR_SERVICE_STARTED = "ScriptService started";
	public static final String LOG_INFO_KILLING_SCRIPTS = "Killing Sensibility Testbed updater and main script";
	public static final String LOG_INFO_SEATTLE_MAIN_SHUTDOWN = "Sensibility Testbed main script shut down";
	public static final String LOG_INFO_STARTING_SEATTLE_MAIN = "Starting Sensibility Testbed main script";
	public static final String LOG_INFO_RESTARTING_SEATTLE_UPDATER = "Restarting Sensibility Testbed updater";
	public static final String LOG_INFO_RESTARTING_SEATTLE_MAIN_AND_UPDATER = "Restarting Sensibility Testbed updater and main";
	public static final String LOG_INFO_SEATTLE_UPDATER_SHUTDOWN = "Sensibility Testbed updater shut down";
	public static final String LOG_INFO_STARTING_SEATTLE_UPDATER = "Starting Sensibility Testbed updater";
	public static final String LOG_INFO_MONITOR_SERVICE_SHUTDOWN = "ScriptService shut down";
	public static final String LOG_INFO_INSTALL_SUCCESS = "Sensibility Testbed installed successfully";
	public static final String LOG_INFO_INSTALL_FAILURE = "Sensibility Testbed could not be installed";
	public static final String LOG_INFO_UNINSTALL_INITIATED = "Sensibility Testbed uninstall initiated by user";
	public static final String LOG_INFO_UNINSTALL_SUCCESS = "Sensibility Testbed uninstalled successfully";
	public static final String LOG_INFO_UNTRUSTED_HOST_CHECK_WHITELIST_OK = "Hostname ok (is on whitelist).";

	// Log -- Verbose info
	public static final String LOG_VERBOSE_EXTRACTING_FILE = "Extracting file: ";
}
