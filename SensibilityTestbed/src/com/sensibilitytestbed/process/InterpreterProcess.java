/*
 * Copyright (C) 2009 Google Inc.
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

package com.sensibilitytestbed.process;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.Environment;

import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.interpreter.Interpreter;
import com.googlecode.android_scripting.interpreter.InterpreterConstants;
import com.googlecode.android_scripting.interpreter.MyInterpreter;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManagerFactory;
import com.sensibilitytestbed.ScriptActivity;
import com.sensibilitytestbed.ScriptApplication;

/**
 * Slightly modified version of the InterpreterProcess from SL4A
 * 
 * This is a skeletal implementation of an interpreter process.
 * 
 * @author Damon Kohler (damonkohler@gmail.com)
 * @modified by Gaetano Pressimone 
 * 				modified to allow embedded python interpreter and scripts in the APK
 * 				based off Anthony Prieur & Daniel Oppenheim work https://code.google.com/p/android-python27/
 */
public class InterpreterProcess extends Process {

  private final AndroidProxy mProxy;
  private final Interpreter mInterpreter;
  private String mCommand;

  private String pyname = "python";
  private File binary = null;
  private String niceName = "Python 2.7.2";
  private String interactiveCommand = "";
  private List<String> arguments = new ArrayList<String>();
  private Map<String, String> environmentVariables = null;
  
  /**
   * Creates a new {@link InterpreterProcess}.
   * 
   * @param launchScript
   *          the absolute path to a script that should be launched with the interpreter
   * @param port
   *          the port that the AndroidProxy is listening on
   */
  public InterpreterProcess(MyInterpreter interpreter, AndroidProxy proxy) {
    mProxy = proxy;
    mInterpreter = interpreter.getInterpreter();

    if(binary != null) {
        setBinary(binary);
    }
    
    setName(niceName);
    setCommand(interactiveCommand);
    addAllArguments(arguments);
  
    putAllEnvironmentVariables(System.getenv());
    putEnvironmentVariable("AP_HOST", getHost());
    putEnvironmentVariable("AP_PORT", Integer.toString(getPort()));
    if (proxy.getSecret() != null) {
      putEnvironmentVariable("AP_HANDSHAKE", getSecret());
    }
    if(environmentVariables != null) {
        putAllEnvironmentVariables(environmentVariables);
    }
  }

  protected void setCommand(String command) {
    mCommand = command;
  }

  public Interpreter getInterpreter() {
    return mInterpreter;
  }

  public String getHost() {
    return mProxy.getAddress().getHostName();
  }

  public int getPort() {
    return mProxy.getAddress().getPort();
  }

  public String getSecret() {
    return mProxy.getSecret();
  }

  public RpcReceiverManagerFactory getRpcReceiverManagerFactory() {
    return mProxy.getRpcReceiverManagerFactory();
  }

  @Override
  public void start(final Runnable shutdownHook) {
	  start(shutdownHook, null);
  }

  public void start(final Runnable shutdownHook, List<String> args) {
    Analytics.track(pyname);
    // NOTE(damonkohler): String.isEmpty() doesn't work on Cupcake.
    if (!mCommand.equals("")) {
      addArgument(mCommand);
    }
    if(args != null)
    	addAllArguments(args);
    super.start(shutdownHook);
  }

  @Override
  public void kill() {
    super.kill();
    mProxy.shutdown();
  }

  @Override
  public String getWorkingDirectory() {
    return InterpreterConstants.SDCARD_SL4A_ROOT;
  }
  @Override
  public String getSdcardPackageDirectory() {
    return ScriptActivity.seattleInstallDirectory.getAbsolutePath() + "/" + ScriptApplication.getThePackageName();
  }
}
