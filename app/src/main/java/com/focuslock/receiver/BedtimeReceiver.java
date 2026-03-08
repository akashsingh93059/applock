package com.focuslock.receiver;

import android.app.*;
import android.content.*;
import androidx.core.app.NotificationCompat;
import com.focuslock.R;
import com.focuslock.ui.MainActivity;
import com.focuslock.utils.SessionManager;

public class BedtimeReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "fl_bedtime";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        SessionManager sm = SessionManager.get(ctx);
        if (sm.hasSessionToday()) return;

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Bedtime Reminder",
            NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(ch);

        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
            new Intent(ctx, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        nm.notify(51, new NotificationCompat.Builder(ctx, CHANNEL)
            .setContentTitle("🌙 Bedtime focus check")
            .setContentText("You haven't focused today. Want a quick 15-min session before bed?")
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).build());
    }
}
