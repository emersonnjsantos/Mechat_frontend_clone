package com.whatsapp.clone.receivers;

import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Handles inline replies sent directly from the Android System Notification bar.
 * Mirrored from com.whatsapp.notification.DirectReplyReceiver in original WhatsApp.
 */
public class DirectReplyReceiver extends BroadcastReceiver {

    private static final String TAG = "DirectReplyReceiver";
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence(KEY_TEXT_REPLY);
            long chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1);
            int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001);

            if (replyText != null) {
                Log.i(TAG, "Received inline direct reply for chat " + chatId + ": " + replyText);

                // Update notification indicating reply was sent
                NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager != null) {
                    NotificationCompat.Builder repliedNotification = new NotificationCompat.Builder(context, "whatsapp_messages")
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentText("Enviado: " + replyText)
                        .setTimeoutAfter(3000);

                    notificationManager.notify(notificationId, repliedNotification.build());
                }
            }
        }
    }
}
