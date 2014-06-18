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

package com.seattletestbed.process;

import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;
import com.googlecode.android_scripting.interpreter.MyInterpreter;

import java.io.File;

/***
 * 
 * Slightly modified version of the PythonScriptProcess
 *
 */
public class PythonScriptProcess extends InterpreterProcess {

  private final File mScript;

  public PythonScriptProcess(File script, InterpreterConfiguration configuration, AndroidProxy proxy) {
    super(new MyInterpreter(null), proxy);
    mScript = script;
    String scriptName = script.getName();
    setName(scriptName);
    String str = "";
    Object[] arrayOfObject = new Object[1];
    arrayOfObject[0] = script.getAbsolutePath();
    setCommand(String.format(str, arrayOfObject));
  }

  public String getPath() {
    return mScript.getPath();
  }

}
