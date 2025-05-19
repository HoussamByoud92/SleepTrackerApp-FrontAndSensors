package com.sleeptracker.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.sleeptracker.R;
import com.sleeptracker.home.HomeActivity;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "sleep_tracker_alarm_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create notification channel for Android O and above
        createNotificationChannel(context);

        // Create intent to open app when notification is tapped
        Intent notificationIntent = new Intent(context, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Get default alarm sound
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("Wake Up!")
                .setContentText("Time to start your day refreshed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(alarmSound)
                .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sleep Tracker Alarms",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for smart sleep alarms");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[] { 1000, 1000, 1000, 1000, 1000 });

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}