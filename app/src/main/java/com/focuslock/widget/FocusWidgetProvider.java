package com.focuslock.widget;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import com.focuslock.R;
import com.focuslock.ui.MainActivity;
import com.focuslock.utils.SessionManager;

public class FocusWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        SessionManager sm = SessionManager.get(ctx);

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_focus);

            rv.setTextViewText(R.id.wStreak, "🔥 " + sm.getStreak() + " day streak");
            int min = sm.getTodayMinutes();
            rv.setTextViewText(R.id.wToday, "⏱ Today: " +
                (min >= 60 ? (min/60) + "h " + (min%60) + "m" : min + " min"));
            rv.setTextViewText(R.id.wTrees, "🌳 " + sm.getTreesPlanted() + " trees");
            rv.setTextViewText(R.id.wCoins, "🪙 " + sm.getCoins());

            boolean active = sm.isSessionActive();
            rv.setTextViewText(R.id.wStatus, active ? "● FOCUSING NOW" : "▶ Tap to focus");
            rv.setInt(R.id.wStatus, "setTextColor", active ? 0xFF6BFFC8 : 0xFF7C6BFF);

            PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.wRoot, pi);

            mgr.updateAppWidget(id, rv);
        }
    }
}
