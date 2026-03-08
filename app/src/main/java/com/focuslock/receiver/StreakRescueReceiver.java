package com.focuslock.receiver;

import android.app.*;
import android.content.*;
import androidx.core.app.NotificationCompat;
import com.focuslock.R;
import com.focuslock.ui.MainActivity;
import com.focuslock.utils.SessionManager;

public class StreakRescueReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "fl_streak";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        SessionManager sm = SessionManager.get(ctx);
        // Only notify if no session today
        if (sm.hasSessionToday()) return;

        int streak = sm.getStreak();
        String title, body;
        if (streak > 0) {
            title = "⚠️ Streak at risk!";
            body = "Your " + streak + "-day streak will break at midnight. Start a quick focus session now!";
        } else {
            title = "🌱 Start your day with focus";
            body = "You haven't focused yet today. Even 5 minutes helps build the habit!";
        }

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Streak Reminders",
            NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(ch);

        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
            new Intent(ctx, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        nm.notify(50, new NotificationCompat.Builder(ctx, CHANNEL)
            .setContentTitle(title).setContentText(body)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build());
    }
}
