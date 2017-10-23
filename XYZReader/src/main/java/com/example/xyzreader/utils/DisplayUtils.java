package com.example.xyzreader.utils;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.Window;

/**
 * Created by Sve on 10/15/17.
 */

public class DisplayUtils {
    public static void useImmersiveStickyFullscreenMode(Window window) {
        window.getDecorView().setSystemUiVisibility
                (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                );
    }

    public static void showSnackbarMessageWithAction(View destinationView, int messageId, int actionId, View.OnClickListener listener) {
        Snackbar mySnackbar = Snackbar.make(
                destinationView,
                messageId,
                Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction(actionId, listener);
        mySnackbar.show();
    }
}
