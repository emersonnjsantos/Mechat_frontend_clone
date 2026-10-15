package com.whatsapp.clone.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Listens for system boot completion to reinitialize background listeners, alarms, and stanzas.
 * Mirrored from com.whatsapp.BootReceiver in original WhatsApp.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Device booted. Re-initializing WhatsApp background push connection...");
            // In a production build, schedule WorkManager background synchronization tasks here
        }
    }
}
