package com.seattletestbed;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/*
 * 
 * Based on: http://www.localytics.com/docs/android-market-campaign-analytics/
 * 
 * 
 */

public class ReferralReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Workaround for Android security issue: http://code.google.com/p/android/issues/detail?id=16006
        try
        {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                extras.containsKey(null);
            }
        }
        catch (final Exception e) {
            return;
        }
 
	Log.i(Common.LOG_TAG, "Refrec started");
        Map<String, String> referralParams = new HashMap<String, String>();
 
        // Return if this is not the right intent.
        if (! intent.getAction().equals("com.android.vending.INSTALL_REFERRER")) { //$NON-NLS-1$
            return;
        }
 
	Log.i(Common.LOG_TAG, "Refrec right intent");
        String referrer = intent.getStringExtra("referrer"); //$NON-NLS-1$
        if( referrer == null || referrer.length() == 0) {
            return;
        }
 	Log.d(Common.LOG_TAG, "Refrec referrer");

        try
        {    // Remove any url encoding
            referrer = URLDecoder.decode(referrer, "utf-8"); //$NON-NLS-1$
        }
        catch (UnsupportedEncodingException e) { return; }
 	Log.i(Common.LOG_TAG, "Refrec right encoding");

        // Parse the query string, extracting the relevant data
        String[] params = referrer.split("&"); // $NON-NLS-1$
        for (String param : params)
        {
            String[] pair = param.split("="); // $NON-NLS-1$
            referralParams.put(pair[0], pair[1]);
        }
 
        ReferralReceiver.storeReferralParams(context, referralParams);
	Log.i(Common.LOG_TAG, "Refrec params added");
    }
 
    private final static String[] EXPECTED_PARAMETERS = {
        "utm_source",
        "utm_medium",
        "utm_term",
        "utm_content",
        "utm_campaign"
    };
    private final static String PREFS_FILE_NAME = "ReferralParamsFile";
 
    /*
     * Stores the referral parameters in the app's sharedPreferences.
     * Rewrite this function and retrieveReferralParams() if a
     * different storage mechanism is preferred.
     */
    public static void storeReferralParams(Context context, Map<String, String> params)
    {
        SharedPreferences storage = context.getSharedPreferences(ReferralReceiver.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = storage.edit();
 
        for(String key : ReferralReceiver.EXPECTED_PARAMETERS)
        {
            String value = params.get(key);
            if(value != null)
            {
                editor.putString(key, value);
            }
        }
 
        editor.commit();

	Log.i(Common.LOG_TAG, Common.LOG_INFO_STORED_REFERRAL_PARAMS + params.toString());
    }
 
    /*
     * Returns a map with the Market Referral parameters pulled from the sharedPreferences.
     */
    public static Map<String, String> retrieveReferralParams(Context context)
    {
        HashMap<String, String> params = new HashMap<String, String>();
        SharedPreferences storage = context.getSharedPreferences(ReferralReceiver.PREFS_FILE_NAME, Context.MODE_PRIVATE);
 
        for(String key : ReferralReceiver.EXPECTED_PARAMETERS)
        {
            String value = storage.getString(key, null);
            if(value != null)
            {
                params.put(key, value);
            }
        }
        return params;
    }
}
