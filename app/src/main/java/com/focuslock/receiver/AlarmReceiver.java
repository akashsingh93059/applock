package com.focuslock.receiver;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.focuslock.R;
import com.focuslock.ui.MainActivity;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ALARM = "fl_alarm";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String label   = intent.getStringExtra("label");
        String toneUri = intent.getStringExtra("toneUri");
        if (label == null) label = "Wake up! ⏰";

        // Vibrate
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createWaveform(
            new long[]{0,600,200,600,200,600}, -1));

        // Resolve sound URI
        Uri soundUri;
        if (toneUri != null && !toneUri.isEmpty()) {
            soundUri = Uri.parse(toneUri);
        } else {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        // Create channel with chosen sound
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        // Delete old channel so sound refreshes
        nm.deleteNotificationChannel(CHANNEL_ALARM + "_" + toneUri);
        String channelId = CHANNEL_ALARM;
        AudioAttributes aa = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        NotificationChannel ch = new NotificationChannel(
            channelId, "Alarms", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(soundUri, aa);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);

        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
            new Intent(ctx, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(ctx, channelId)
            .setContentTitle("⏰ Alarm!")
            .setContentText(label)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .build();

        nm.notify((int)(System.currentTimeMillis() % Integer.MAX_VALUE), n);
    }
}
