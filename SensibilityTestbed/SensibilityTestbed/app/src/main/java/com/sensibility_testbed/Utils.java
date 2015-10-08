package com.sensibility_testbed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.googlecode.android_scripting.FileUtils;

/***
 * Utility class
 *
 * Extracts zip archives, create and deletes directories
 *
 * based off Anthony Prieur & Daniel Oppenheim work
 * https://code.google.com/p/android-python27/
 *
 */
public class Utils {
  public static String getFileExtension(String sFileName) {
    int dotIndex = sFileName.lastIndexOf('.');
    if (dotIndex == -1) {
      return null;
    }
    return sFileName.substring(dotIndex);
  }

  public static void unzip(InputStream inputStream, String dest,
      boolean replaceIfExists) throws Exception {

    final int BUFFER_SIZE = 4096;
    BufferedOutputStream bufferedOutputStream = null;

    try {
      ZipInputStream zipInputStream = new ZipInputStream(
          new BufferedInputStream(inputStream));
      ZipEntry zipEntry;

      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        String zipEntryName = zipEntry.getName();
        // Check if file exist, if it does then delete
        File file2 = new File(dest + zipEntryName);

        if (file2.exists()) {
          if (replaceIfExists) {
            try {
              boolean b = deleteDir(file2);
              if (!b) {
                Log.e(Common.LOG_TAG, "Unzip failed to delete " + dest
                    + zipEntryName);
              } else {
                Log.i(Common.LOG_TAG, "Unzip deleted " + dest + zipEntryName);
              }
            } catch (Exception e) {
              Log.e(Common.LOG_TAG, "Unzip failed to delete " + dest
                  + zipEntryName, e);
            }
          }
        }
        // extract
        File file = new File(dest + zipEntryName);

        if (file.exists()) {

        } else {
          if (zipEntry.isDirectory()) {
            file.mkdirs();
            FileUtils.chmod(file, 0755);

          } else {

            // Create parent file folder if not exists yet
            if (!file.getParentFile().exists()) {
              file.getParentFile().mkdirs();
              FileUtils.chmod(file.getParentFile(), 0755);
            }

            byte buffer[] = new byte[BUFFER_SIZE];
            bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(file), BUFFER_SIZE);
            int count;

            while ((count = zipInputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
              bufferedOutputStream.write(buffer, 0, count);
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
          }
        }

        // Set permission for python files
        if (file.getName().endsWith(".so") || file.getName().endsWith(".py")
            || file.getName().endsWith(".pyc")
            || file.getName().endsWith(".pyo")) {
          FileUtils.chmod(file, 0755);
        }
        Log.i(Common.LOG_TAG, "Unzip extracted " + dest + zipEntryName);
      }
      zipInputStream.close();

    } catch (FileNotFoundException e) {
      Log.e(Common.LOG_TAG, "Unzip error, file not found", e);
      throw e;
    } catch (Exception e) {
      Log.e(Common.LOG_TAG, "Unzip error: ", e);
      throw e;
    }

  }

  public static boolean deleteDir(File dir) {
    try {
      if (dir.isDirectory()) {
        String[] children = dir.list();
        // Delete everything in directory
        for (int i = 0; i < children.length; i++) {
          boolean success = deleteDir(new File(dir, children[i]));
          if (!success) {
            return false;
          }
        }
      }
      // The directory is now empty so delete the folder
      return dir.delete();

    } catch (Exception e) {
      Log.e(Common.LOG_TAG, "Failed to delete " + dir + " : " + e);
      return false;
    }
  }

  public static void createDirectoryOnExternalStorage(String path) {
    try {
      File file = new File(ScriptActivity.seattleInstallDirectory, path);
      if (!file.exists()) {
        try {
          file.mkdirs();
          Log.i(Common.LOG_TAG, "createDirectoryOnExternalStorage created "
              + ScriptActivity.seattleInstallDirectory.getAbsolutePath() + "/"
              + path);
        } catch (Exception e) {
          Log.e(Common.LOG_TAG, "createDirectoryOnExternalStorage error: ", e);
        }
      }
    } catch (Exception e) {
      Log.e(Common.LOG_TAG, "createDirectoryOnExternalStorage error: " + e);
    }
  }

  // check if an app is installed, thanks to
  // http://www.grokkingandroid.com/checking-intent-availability/
  public static boolean isMyServiceInstalled(Context ctx, Intent intent) {
    final PackageManager mgr = ctx.getPackageManager();
    List<ResolveInfo> list = mgr.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY);
    return list.size() > 0;
  }

}