package com.focuslock.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.focuslock.R;
import com.focuslock.utils.SessionManager;

public class ForestActivity extends AppCompatActivity {
    private SessionManager sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forest);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("🌳 My Forest");
        sm = SessionManager.get(this);
        render();
    }

    private void render() {
        int trees = sm.getTreesPlanted();
        int coins = sm.getCoins();
        int sessions = sm.getTotalSessions();
        int minutes = sm.getTotalMinutes();

        ((TextView) findViewById(R.id.tvForestTitle)).setText(
            trees == 0 ? "Your forest is empty" :
            trees < 5  ? "A sapling grove 🌱" :
            trees < 15 ? "A growing forest 🌲" :
            trees < 30 ? "A thriving woodland 🌳" :
                         "An ancient forest 🏔️🌲");

        ((TextView) findViewById(R.id.tvForestStats)).setText(
            "🌳 " + trees + " trees planted  ·  🪙 " + coins + " coins  ·  ⏱ " +
            (minutes >= 60 ? (minutes/60) + "h " + (minutes%60) + "m" : minutes + "m") + " focused");

        // Build tree grid
        GridLayout grid = findViewById(R.id.gridForest);
        grid.removeAllViews();
        grid.setColumnCount(7);

        for (int i = 0; i < Math.max(trees, 21); i++) {
            TextView tv = new TextView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = dpToPx(40);
            lp.height = dpToPx(40);
            lp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            tv.setLayoutParams(lp);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(22f);

            if (i < trees) {
                // Pick tree emoji by milestone
                if (trees >= 30 && i < trees) tv.setText("🌲");
                else if (trees >= 15 && i < trees) tv.setText("🌳");
                else if (trees >= 5 && i < trees)  tv.setText("🌴");
                else tv.setText("🌱");
                tv.setAlpha(1f);
            } else {
                tv.setText("⬜");
                tv.setAlpha(0.15f);
            }
            grid.addView(tv);
        }

        // Next tree progress
        int progressToNextTree = sm.getCoins();
        int need = 500;
        int pct = Math.min(100, (progressToNextTree * 100) / need);
        ((ProgressBar) findViewById(R.id.pbNextTree)).setProgress(pct);
        ((TextView) findViewById(R.id.tvNextTreeLabel)).setText(
            coins >= 500 ? "✅ Ready to plant! Go to a focus session to plant." :
            "🌱 " + coins + " / 500 coins to next tree  (" + pct + "%)");

        // Milestone badges
        LinearLayout ll = findViewById(R.id.llMilestones);
        ll.removeAllViews();
        String[][] badges = {
            {"1 tree","🌱","1"},{"5 trees","🌿","5"},{"10 trees","🌲","10"},
            {"25 trees","🌳","25"},{"50 trees","🏕️","50"},{"100 trees","🌏","100"}
        };
        for (String[] b : badges) {
            TextView badge = new TextView(this);
            badge.setText(b[1] + " " + b[0]);
            badge.setTextSize(13f);
            badge.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            badge.setTextColor(trees >= Integer.parseInt(b[2]) ? 0xFF66BB6A : 0xFF333344);
            badge.setBackgroundColor(trees >= Integer.parseInt(b[2]) ? 0xFF1A2A1A : 0xFF111118);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dpToPx(8), dpToPx(8));
            badge.setLayoutParams(lp);
            ll.addView(badge);
        }
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}
