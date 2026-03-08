package com.focuslock.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.*;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.focuslock.R;
import com.focuslock.utils.SessionManager;
import java.util.Locale;

public class BlockedActivity extends Activity {
    private CountDownTimer refresher;
    private SessionManager sm;
    private TextView tvCoinBalance;
    private TextView tvTreeBalance;
    private Button btnPlantTree;
    private Button btnCutTree;

    private static final String[] QUOTES = {
        "📚 Stay focused — your goals need you now!",
        "💪 Discipline is choosing between what you want NOW and what you want MOST.",
        "🔥 Every minute you resist builds your future.",
        "🎯 Champions don't stop when it's hard. They stop when it's DONE.",
        "🌟 Close this. Go back to studying. You've got this.",
        "⚡ Your future self is watching. Don't let them down.",
        "🏆 One day or day one. You decide.",
        "🌳 Plant a tree today. Your future self will thank you.",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.activity_blocked);

        sm = SessionManager.get(this);
        String pkg        = getIntent().getStringExtra("blocked_pkg");
        long remainMs     = getIntent().getLongExtra("remaining_ms", 0);
        String mode       = getIntent().getStringExtra("mode");

        // App name
        String appName = pkg;
        try {
            appName = getPackageManager()
                .getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ignored) {}

        ((TextView) findViewById(R.id.tvBlockedName)).setText("🚫 " + appName + " is blocked");

        String quote = QUOTES[(int)(System.currentTimeMillis() % QUOTES.length)];
        ((TextView) findViewById(R.id.tvQuote)).setText(quote);

        if ("schedule".equals(mode)) {
            ((TextView) findViewById(R.id.tvModeInfo)).setText(
                "📅 Schedule blocking is active.\nOnly allowed apps may be used right now.");
        } else {
            ((TextView) findViewById(R.id.tvModeInfo)).setText(
                "This app is blocked during your focus session.");
        }

        // Setup coin and tree UI
        tvCoinBalance = findViewById(R.id.tvCoinBalance);
        tvTreeBalance = findViewById(R.id.tvTreeBalance);
        btnPlantTree = findViewById(R.id.btnPlantTree);
        btnCutTree = findViewById(R.id.btnCutTree);
        updateCoinAndTreeUI();

        // Countdown
        startRefresher(remainMs);

        // Go home
        findViewById(R.id.btnGoHome).setOnClickListener(v -> goHome());

        // Plant tree
        btnPlantTree.setOnClickListener(v -> plantTree());

        // Cut tree
        btnCutTree.setOnClickListener(v -> cutTree());

        // End session
        findViewById(R.id.btnEndSession).setOnClickListener(v -> confirmEnd());
    }

    private void updateCoinAndTreeUI() {
        int coins = sm.getCoins();
        int trees = sm.getTreesPlanted();
        
        tvCoinBalance.setText("🪙 " + coins + " coin" + (coins == 1 ? "" : "s"));
        tvTreeBalance.setText("🌳 " + trees + " tree" + (trees == 1 ? "" : "s"));
        
        // Enable/disable plant tree button based on coin balance
        if (coins >= 500) {
            btnPlantTree.setEnabled(true);
            btnPlantTree.setText("🌱 Plant Tree (500 Coins)");
        } else {
            btnPlantTree.setEnabled(false);
            btnPlantTree.setText("🌱 Need " + (500 - coins) + " more coins");
        }
        
        // Enable/disable cut tree button based on tree count
        if (trees > 0) {
            btnCutTree.setEnabled(true);
            btnCutTree.setText("🪓 Cut Tree (Get 500 Coins)");
        } else {
            btnCutTree.setEnabled(false);
            btnCutTree.setText("🪓 No trees to cut");
        }
    }

    private void plantTree() {
        int coins = sm.getCoins();
        if (coins < 500) {
            new AlertDialog.Builder(this)
                .setTitle("🌱 Not Enough Coins")
                .setMessage("You need 500 coins to plant a tree, but you only have " + coins + " coins.\n\nKeep focusing! Every completed session earns you coins.\n\nYou need " + (500 - coins) + " more coins. You're almost there! 💪")
                .setPositiveButton("Back to focusing!", null)
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("🌱 Plant a Tree in Your Forest?")
            .setMessage("Spend 500 coins to plant a real virtual tree that represents your dedication.\n\nThis tree is a trophy of your focus. It will stand in your forest as a reminder of every distraction you defeated.\n\nReady to grow your forest? 🌳")
            .setPositiveButton("Yes! Plant my tree! 🌳", (d, w) -> {
                if (sm.plantTree()) {
                    new AlertDialog.Builder(this)
                        .setTitle("🎉 Tree Planted!")
                        .setMessage("Beautiful! A new tree is growing in your focus forest! 🌱🌳\n\nYou've earned this through real discipline and hard work. Your forest is growing stronger, just like you.\n\nKeep focusing — every session plants the seeds of a better you! 🌟")
                        .setPositiveButton("I love my forest! 🌳", null)
                        .show();
                    updateCoinAndTreeUI();
                }
            })
            .setNegativeButton("Maybe later", null)
            .show();
    }

    private void cutTree() {
        int trees = sm.getTreesPlanted();
        if (trees <= 0) {
            new AlertDialog.Builder(this)
                .setTitle("🌱 No Trees Yet")
                .setMessage("You haven't planted any trees yet.\n\nEarn coins by completing focus sessions, then spend 500 coins to plant a tree in your virtual forest.\n\nYour focus builds something real. Start growing! 🌳")
                .setPositiveButton("I'll focus more!", null)
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("🪓 Cut a Tree?")
            .setMessage("⚠️ Are you sure you want to cut a tree?\n\nEach tree represents hours of real focus and discipline you put in. Cutting it means trading your hard-earned achievement for 500 coins to bypass a block.\n\nThis will let you access the blocked app — but at the cost of your forest. 😔\n\nIs it worth it?")
            .setPositiveButton("Yes, cut it (I regret this)", (d, w) -> {
                int coinsGained = sm.cutTree();
                if (coinsGained > 0) {
                    new AlertDialog.Builder(this)
                        .setTitle("😔 Tree Cut — Forest Diminished")
                        .setMessage("You gained " + coinsGained + " coins, but a tree from your forest is gone forever.\n\n\"The tree that takes years to grow can be cut in a minute. Your focus is the same.\"\n\nYou'll need to earn those hours back. Let this be a reminder of how precious your focus time really is. 🌱 Start fresh and grow again.")
                        .setPositiveButton("I understand, I'll do better", null)
                        .show();
                    updateCoinAndTreeUI();
                }
            })
            .setNegativeButton("No, keep my forest 🌳", null)
            .show();
    }

    private void startRefresher(long initMs) {
        updateCountdown(initMs);
        refresher = new CountDownTimer(initMs, 1000) {
            public void onTick(long ms) { updateCountdown(ms); }
            public void onFinish() { finish(); }
        }.start();
    }

    private void updateCountdown(long ms) {
        long m = ms/60_000, s=(ms%60_000)/1_000;
        ((TextView)findViewById(R.id.tvCountdown)).setText(
            String.format(Locale.getDefault(), "%02d:%02d remaining", m, s));
    }

    private void confirmEnd() {
        new AlertDialog.Builder(this)
            .setTitle("End focus session?")
            .setNegativeButton("Keep focusing 💪", null)
            .setPositiveButton("End", (d,w) -> {
                startService(new Intent(this,
                    com.focuslock.service.AppMonitorService.class)
                    .setAction(com.focuslock.service.AppMonitorService.ACTION_STOP));
                com.focuslock.model.FocusSession fs = sm.loadSession();
                fs.setActive(false);
                sm.saveSession(fs);
                goHome();
            }).show();
    }

    private void goHome() {
        Intent h = new Intent(Intent.ACTION_MAIN);
        h.addCategory(Intent.CATEGORY_HOME);
        h.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(h);
        finish();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req==100 && res==RESULT_OK && data!=null && data.getBooleanExtra("verified",false))
            confirmEnd();
    }

    @Override public void onBackPressed() { goHome(); }

    @Override protected void onDestroy() {
        if (refresher != null) refresher.cancel();
        super.onDestroy();
    }
}
