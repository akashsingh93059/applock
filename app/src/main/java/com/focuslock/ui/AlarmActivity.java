package com.focuslock.ui;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.focuslock.R;
import com.focuslock.receiver.AlarmReceiver;
import java.util.*;

public class AlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private EditText etAlarmLabel;
    private ListView lvAlarms;
    private AlarmManager alarmManager;
    private List<AlarmItem> alarms = new ArrayList<>();
    private AlarmAdapter adapter;
    private MediaPlayer previewPlayer;

    // Tone selection
    private String selectedToneUri  = "";  // empty = system default
    private String selectedToneName = "Default alarm";
    private TextView tvSelectedTone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("⏰ Alarms");

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        timePicker    = findViewById(R.id.tpAlarm);
        etAlarmLabel  = findViewById(R.id.etAlarmLabel);
        lvAlarms      = findViewById(R.id.lvAlarms);
        tvSelectedTone= findViewById(R.id.tvSelectedTone);
        timePicker.setIs24HourView(false);

        loadAlarms();
        adapter = new AlarmAdapter();
        lvAlarms.setAdapter(adapter);

        lvAlarms.setOnItemLongClickListener((parent, view, pos, id) -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete alarm?")
                .setPositiveButton("Delete", (d, w) -> deleteAlarm(pos))
                .setNegativeButton("Cancel", null).show();
            return true;
        });

        findViewById(R.id.btnSetAlarm).setOnClickListener(v -> setAlarm());
        findViewById(R.id.btnPickTone).setOnClickListener(v -> showTonePicker());
        updateToneLabel();
    }

    // ── Tone Picker ──────────────────────────────────────
    private void showTonePicker() {
        String[] options = {"System default alarm", "Pick ringtone", "Pick music from device"};
        new AlertDialog.Builder(this)
            .setTitle("Choose alarm sound")
            .setItems(options, (d, which) -> {
                if (which == 0) {
                    selectedToneUri  = "";
                    selectedToneName = "Default alarm";
                    updateToneLabel();
                } else if (which == 1) {
                    Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                    i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone");
                    if (!selectedToneUri.isEmpty())
                        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedToneUri));
                    startActivityForResult(i, 901);
                } else {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.setType("audio/*");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(i, 902);
                }
            }).show();
    }

    private void previewTone() {
        stopPreview();
        try {
            Uri uri = selectedToneUri.isEmpty()
                ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                : Uri.parse(selectedToneUri);
            previewPlayer = new MediaPlayer();
            previewPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());
            previewPlayer.setDataSource(this, uri);
            previewPlayer.setLooping(false);
            previewPlayer.prepare();
            previewPlayer.start();
            previewPlayer.setOnCompletionListener(mp -> stopPreview());
        } catch (Exception e) {
            Toast.makeText(this, "Cannot preview this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPreview() {
        if (previewPlayer != null) {
            try { previewPlayer.stop(); previewPlayer.release(); } catch (Exception ignored) {}
            previewPlayer = null;
        }
    }

    private void updateToneLabel() {
        tvSelectedTone.setText("🎵 " + selectedToneName);
    }

    // ── Set Alarm ────────────────────────────────────────
    private void setAlarm() {
        int hour   = timePicker.getHour();
        int minute = timePicker.getMinute();
        String label = etAlarmLabel.getText().toString().trim();
        if (label.isEmpty()) label = "Wake up ⏰";

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);

        int rc = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
        AlarmItem item = new AlarmItem(rc, hour, minute, label, cal.getTimeInMillis(), selectedToneUri, selectedToneName);
        alarms.add(item);
        saveAlarms();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("label", label);
        intent.putExtra("toneUri", selectedToneUri);
        PendingIntent pi = PendingIntent.getBroadcast(this, rc, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }

        String amPm = hour < 12 ? "AM" : "PM";
        int dh = hour % 12; if (dh == 0) dh = 12;
        Toast.makeText(this, "⏰ Alarm set for " +
            String.format(Locale.getDefault(), "%d:%02d %s", dh, minute, amPm), Toast.LENGTH_LONG).show();
        adapter.notifyDataSetChanged();
        refreshEmptyView();
        etAlarmLabel.setText("");
    }

    private void deleteAlarm(int pos) {
        AlarmItem item = alarms.get(pos);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, item.requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pi != null) alarmManager.cancel(pi);
        alarms.remove(pos);
        saveAlarms();
        adapter.notifyDataSetChanged();
        refreshEmptyView();
    }

    private void refreshEmptyView() {
        TextView tvEmpty = findViewById(R.id.tvNoAlarms);
        tvEmpty.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Persistence ──────────────────────────────────────
    private void saveAlarms() {
        SharedPreferences prefs = getSharedPreferences("focuslock_alarms", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (AlarmItem a : alarms) {
            sb.append(a.requestCode).append(",")
              .append(a.hour).append(",").append(a.minute).append(",")
              .append(a.triggerMs).append(",")
              .append(a.toneUri.replace("|","")).append(",")
              .append(a.toneName.replace("|","")).append(",")
              .append(a.label.replace("|","")).append("|");
        }
        prefs.edit().putString("alarms2", sb.toString()).apply();
    }

    private void loadAlarms() {
        SharedPreferences prefs = getSharedPreferences("focuslock_alarms", MODE_PRIVATE);
        String data = prefs.getString("alarms2", "");
        alarms.clear();
        if (!data.isEmpty()) {
            for (String part : data.split("\\|")) {
                if (part.isEmpty()) continue;
                try {
                    String[] f = part.split(",", 7);
                    String toneUri  = f.length > 4 ? f[4] : "";
                    String toneName = f.length > 5 ? f[5] : "Default";
                    String lbl      = f.length > 6 ? f[6] : "Wake up";
                    alarms.add(new AlarmItem(Integer.parseInt(f[0]),
                        Integer.parseInt(f[1]), Integer.parseInt(f[2]),
                        lbl, Long.parseLong(f[3]), toneUri, toneName));
                } catch (Exception ignored) {}
            }
        }
    }

    // ── Adapter ───────────────────────────────────────────
    class AlarmAdapter extends BaseAdapter {
        public int getCount() { return alarms.size(); }
        public Object getItem(int i) { return alarms.get(i); }
        public long getItemId(int i) { return i; }
        public android.view.View getView(int pos, android.view.View v, android.view.ViewGroup parent) {
            if (v == null) v = getLayoutInflater().inflate(R.layout.item_alarm, parent, false);
            AlarmItem a = alarms.get(pos);
            String amPm = a.hour < 12 ? "AM" : "PM";
            int dh = a.hour % 12; if (dh == 0) dh = 12;
            ((TextView) v.findViewById(R.id.tvAlarmTime)).setText(
                String.format(Locale.getDefault(), "%d:%02d %s", dh, a.minute, amPm));
            ((TextView) v.findViewById(R.id.tvAlarmLabel)).setText(a.label);
            ((TextView) v.findViewById(R.id.tvAlarmTone)).setText("🎵 " + a.toneName);
            v.findViewById(R.id.btnPreviewAlarm).setOnClickListener(vv -> {
                stopPreview();
                selectedToneUri = a.toneUri;
                previewTone();
            });
            return v;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        if (req == 901) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedToneUri = uri.toString();
                Ringtone r = RingtoneManager.getRingtone(this, uri);
                selectedToneName = r != null ? r.getTitle(this) : "Custom ringtone";
            }
        } else if (req == 902) {
            Uri uri = data.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                selectedToneUri = uri.toString();
                selectedToneName = getFileName(uri);
            }
        }
        updateToneLabel();
    }

    private String getFileName(Uri uri) {
        String name = "Custom music";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        // Remove extension
        if (name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
        return name;
    }

    @Override protected void onDestroy() { stopPreview(); super.onDestroy(); }

    // ── Data class ────────────────────────────────────────
    static class AlarmItem {
        int requestCode, hour, minute;
        String label, toneUri, toneName;
        long triggerMs;
        AlarmItem(int rc, int h, int m, String l, long t, String tu, String tn) {
            requestCode=rc; hour=h; minute=m; label=l; triggerMs=t; toneUri=tu; toneName=tn;
        }
    }
}
