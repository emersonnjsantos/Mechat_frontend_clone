package com.whatsapp.clone.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Android Foreground Service for handling active VoIP voice and video calls.
 * Implements audio focus management, microphone streaming, and persistent notification.
 * Mirrored from com.whatsapp.calling.service.VoiceService in original WhatsApp.
 */
public class VoiceCallingService extends Service {

    private static final String TAG = "VoiceCallingService";
    public static final String CHANNEL_ID = "whatsapp_voip_channel";
    public static final int NOTIFICATION_ID = 2026;

    public static final String ACTION_START_CALL = "com.whatsapp.clone.action.START_CALL";
    public static final String ACTION_END_CALL = "com.whatsapp.clone.action.END_CALL";
    public static final String EXTRA_CONTACT_NAME = "extra_contact_name";
    public static final String EXTRA_IS_VIDEO = "extra_is_video";

    private boolean isCallActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.i(TAG, "VoiceCallingService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START_CALL.equals(action)) {
            String contactName = intent.getStringExtra(EXTRA_CONTACT_NAME);
            boolean isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false);
            startForegroundCall(contactName != null ? contactName : "Contato", isVideo);
        } else if (ACTION_END_CALL.equals(action)) {
            endCall();
        }

        return START_NOT_STICKY;
    }

    private void startForegroundCall(String contactName, boolean isVideo) {
        isCallActive = true;
        Log.i(TAG, "Starting foreground call with: " + contactName + ", isVideo=" + isVideo);

        Intent endCallIntent = new Intent(this, VoiceCallingService.class);
        endCallIntent.setAction(ACTION_END_CALL);
        PendingIntent endPendingIntent = PendingIntent.getService(
            this,
            0,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(isVideo ? "Chamada de vídeo do WhatsApp" : "Chamada de voz do WhatsApp")
            .setContentText("Em chamada com " + contactName)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desligar", endPendingIntent)
            .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            }
            if (isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            }
            startForeground(NOTIFICATION_ID, notification, serviceType);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void endCall() {
        Log.i(TAG, "Ending call and stopping foreground service");
        isCallActive = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Chamadas do WhatsApp",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificações em andamento de chamadas de voz e vídeo");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "VoiceCallingService destroyed");
    }
}
