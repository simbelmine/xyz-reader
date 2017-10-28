package com.example.xyzreader.remote;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.xyzreader.data.UpdaterService;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    static {
        URL url = null;
        try {
            url = new URL("https://go.udacity.com/xyz-reader-json" );
        } catch (MalformedURLException ignored) {
            Log.e("xyz", "Please check your internet connection.");

            Context context = UpdaterService.UPDATE_SERVICE_CONTEXT;
            if (context != null) {
                Intent intent = new Intent("xyz");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }

        BASE_URL = url;
    }
}
