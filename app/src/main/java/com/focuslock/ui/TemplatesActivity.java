package com.focuslock.ui;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.focuslock.R;
import com.focuslock.model.FocusSession;
import com.focuslock.utils.SessionManager;
import com.google.gson.Gson;
import java.util.*;

public class TemplatesActivity extends AppCompatActivity {

    private SessionManager sm;
    private List<Template> templates = new ArrayList<>();
    private LinearLayout llTemplates;
    private Gson gson = new Gson();

    public static class Template {
        public String name, icon;
        public int durationMinutes;
        public List<String> blockedApps;
        public Template(String name, String icon, int dur, List<String> apps) {
            this.name=name; this.icon=icon; this.durationMinutes=dur; this.blockedApps=apps;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_templates);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("📋 Session Templates");
        sm = SessionManager.get(this);
        llTemplates = findViewById(R.id.llTemplates);
        loadTemplates();
        renderTemplates();

        findViewById(R.id.btnNewTemplate).setOnClickListener(v -> showCreateDialog());
    }

    private void loadTemplates() {
        String json = sm.getTemplatesJson();
        if (json == null || json.isEmpty()) {
            // Default templates
            templates.add(new Template("Deep Work", "🧠", 90,
                Arrays.asList("com.google.android.youtube","com.instagram.android","com.facebook.katana","com.twitter.android","com.zhiliaoapp.musically")));
            templates.add(new Template("Study Session", "📚", 45,
                Arrays.asList("com.google.android.youtube","com.instagram.android","com.facebook.katana","com.twitter.android","com.snapchat.android","com.zhiliaoapp.musically")));
            templates.add(new Template("Quick Focus", "⚡", 25,
                Arrays.asList("com.google.android.youtube","com.instagram.android")));
            templates.add(new Template("Morning Routine", "🌅", 60,
                Arrays.asList("com.google.android.youtube","com.facebook.katana","com.reddit.frontpage")));
            saveTemplates();
        } else {
            try {
                Template[] arr = gson.fromJson(json, Template[].class);
                templates = new ArrayList<>(Arrays.asList(arr));
            } catch (Exception e) { templates = new ArrayList<>(); }
        }
    }

    private void saveTemplates() {
        sm.saveTemplatesJson(gson.toJson(templates.toArray()));
    }

    private void renderTemplates() {
        llTemplates.removeAllViews();
        for (int i = 0; i < templates.size(); i++) {
            final int idx = i;
            Template t = templates.get(i);
            View card = LayoutInflater.from(this).inflate(R.layout.item_template, llTemplates, false);
            ((TextView) card.findViewById(R.id.tvTemplateIcon)).setText(t.icon);
            ((TextView) card.findViewById(R.id.tvTemplateName)).setText(t.name);
            ((TextView) card.findViewById(R.id.tvTemplateDesc)).setText(
                t.durationMinutes + " min  ·  " + t.blockedApps.size() + " apps blocked");
            card.findViewById(R.id.btnApplyTemplate).setOnClickListener(v -> applyTemplate(t));
            card.findViewById(R.id.btnDeleteTemplate).setOnClickListener(v -> {
                templates.remove(idx);
                saveTemplates();
                renderTemplates();
            });
            llTemplates.addView(card);
        }
    }

    private void applyTemplate(Template t) {
        FocusSession s = sm.loadSession();
        s.setDurationMinutes(t.durationMinutes);
        s.setBlockedApps(new ArrayList<>(t.blockedApps));
        sm.saveSession(s);
        Toast.makeText(this, "✅ Template applied: " + t.name, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showCreateDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_new_template, null);
        EditText etName = v.findViewById(R.id.etTplName);
        EditText etIcon = v.findViewById(R.id.etTplIcon);
        SeekBar sbDur  = v.findViewById(R.id.sbTplDuration);
        TextView tvDur = v.findViewById(R.id.tvTplDuration);
        sbDur.setMax(115);
        sbDur.setProgress(20);
        tvDur.setText("25 min");
        sbDur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { tvDur.setText((p+5)+" min"); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        new AlertDialog.Builder(this)
            .setTitle("New Template")
            .setView(v)
            .setPositiveButton("Create", (d, w) -> {
                String name = etName.getText().toString().trim();
                String icon = etIcon.getText().toString().trim();
                if (name.isEmpty()) name = "Custom";
                if (icon.isEmpty()) icon = "🎯";
                int dur = sbDur.getProgress() + 5;
                // Use current session's blocked apps as starting point
                FocusSession s = sm.loadSession();
                templates.add(new Template(name, icon, dur, new ArrayList<>(s.getBlockedApps())));
                saveTemplates();
                renderTemplates();
                Toast.makeText(this, "Template created!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
