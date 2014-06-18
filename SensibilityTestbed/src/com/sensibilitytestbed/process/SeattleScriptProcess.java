package com.sensibilitytestbed.process;

import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

/***
 * 
 * Slightly modified version of the ScriptProcess from SL4A
 * with the possibility to launch scripts with
 * command-line arguments and environmental variables,
 * and to specify the working directory of the script at
 * launch time
 *
 */
public class SeattleScriptProcess extends PythonScriptProcess{

	public static SeattleScriptProcess launchScript(File script,
			InterpreterConfiguration configuration, final AndroidProxy proxy,
			Runnable shutdownHook, String workingDirectory,
			String sdcardPackageDirectory, List<String> args,
			Map<String, String> environment, File binary) {
		
		if (!script.exists()) {
			throw new RuntimeException("No such script to launch.");
		}
		SeattleScriptProcess process = new SeattleScriptProcess(script,
				configuration, proxy, workingDirectory, sdcardPackageDirectory);
		if(environment != null)
			process.putAllEnvironmentVariables(environment);
		process.setBinary(binary);
		if (shutdownHook == null) {
			process.start(new Runnable() {
				@Override
				public void run() {
					proxy.shutdown();
				}
			}, args);
		} else {
			process.start(shutdownHook, args);
		}
		return process;
	}
	
	private String workingDirectory;
	private String sdcardPackageDirectory;

	private SeattleScriptProcess(File script,
			InterpreterConfiguration configuration, AndroidProxy proxy,
			String workingDirectory, String sdcardPackageDirectory) {
		super(script, configuration, proxy);
		
		this.workingDirectory = workingDirectory;
		this.sdcardPackageDirectory = sdcardPackageDirectory;
	}

	@Override
	public String getWorkingDirectory() {
	   return workingDirectory;
	}
	
	@Override
	public String getSdcardPackageDirectory(){
		return sdcardPackageDirectory;
	}
}
