package com.focuslock.ui;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.focuslock.R;
import com.focuslock.utils.SessionManager;

public class FocusScoreActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_score);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("🎯 Focus Score");
        SessionManager sm = SessionManager.get(this);

        int streak       = sm.getStreak();
        int sessions     = sm.getTotalSessions();
        int minutes      = sm.getTotalMinutes();
        int trees        = sm.getTreesPlanted();
        int todayMinutes = sm.getTodayMinutes();

        // Score formula: streak(30) + sessions(20) + hours(25) + trees(15) + today(10)
        int streakScore   = Math.min(30, streak * 3);
        int sessionScore  = Math.min(20, sessions * 2);
        int hoursScore    = Math.min(25, (minutes / 60) * 2);
        int treeScore     = Math.min(15, trees * 2);
        int todayScore    = Math.min(10, (todayMinutes / 5));
        int total         = streakScore + sessionScore + hoursScore + treeScore + todayScore;

        String grade = total >= 90 ? "S" : total >= 75 ? "A" : total >= 55 ? "B" :
                       total >= 35 ? "C" : total >= 15 ? "D" : "F";
        String gradeColor = total >= 90 ? "#FFD700" : total >= 75 ? "#7C6BFF" :
                            total >= 55 ? "#6BFFC8" : total >= 35 ? "#FFB347" :
                            total >= 15 ? "#FF6B9D" : "#888888";

        ((TextView) findViewById(R.id.tvScoreGrade)).setText(grade);
        ((TextView) findViewById(R.id.tvScoreGrade)).setTextColor(android.graphics.Color.parseColor(gradeColor));
        ((TextView) findViewById(R.id.tvScoreTotal)).setText(total + " / 100");

        ((ProgressBar) findViewById(R.id.pbScore)).setProgress(total);

        String tip = total >= 90 ? "You're a focus legend! Keep growing your forest! 🏆" :
                     total >= 75 ? "Excellent discipline! One more push for perfection! 🌟" :
                     total >= 55 ? "Good work! Build your streak to unlock higher scores. 🔥" :
                     total >= 35 ? "You're on the right track. More sessions = more score. 💪" :
                                   "Start a focus session today to boost your score! 🌱";
        ((TextView) findViewById(R.id.tvScoreTip)).setText(tip);

        // Breakdown
        setBar(R.id.pbStreak,   R.id.tvStreakScore,   "Streak",    streakScore,  30, streak + " day streak");
        setBar(R.id.pbSessions, R.id.tvSessionScore,  "Sessions",  sessionScore, 20, sessions + " sessions done");
        setBar(R.id.pbHours,    R.id.tvHoursScore,    "Hours",     hoursScore,   25, (minutes/60) + "h focused");
        setBar(R.id.pbTrees,    R.id.tvTreesScore,    "Forest",    treeScore,    15, trees + " trees planted");
        setBar(R.id.pbToday,    R.id.tvTodayScore,    "Today",     todayScore,   10, todayMinutes + " min today");
    }

    private void setBar(int pbId, int tvId, String label, int val, int max, String detail) {
        ProgressBar pb = findViewById(pbId);
        TextView tv    = findViewById(tvId);
        pb.setMax(max);
        pb.setProgress(val);
        tv.setText(label + "  " + val + "/" + max + "  (" + detail + ")");
    }
}
