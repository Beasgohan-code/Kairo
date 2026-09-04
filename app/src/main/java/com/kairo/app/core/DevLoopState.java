package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists Dev Loop progress across messages on this device. */
public final class DevLoopState {
    private static final String PREFS = "kairo_dev_loop";
    private static final String KEY_PHASE = "phase"; // 0..6
    private static final String KEY_TASK = "task";
    private static final String KEY_NOTES = "notes";

    public static final String[] PHASES = {
            "Plan", "Code", "Test", "Review", "Edit", "Debug", "Done"
    };

    private final SharedPreferences preferences;

    public DevLoopState(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getPhase() {
        return Math.max(0, Math.min(PHASES.length - 1, preferences.getInt(KEY_PHASE, 0)));
    }

    public void setPhase(int phase) {
        preferences.edit().putInt(KEY_PHASE, Math.max(0, Math.min(PHASES.length - 1, phase))).apply();
    }

    public void advance() {
        int next = Math.min(PHASES.length - 1, getPhase() + 1);
        setPhase(next);
    }

    public String getTask() {
        return preferences.getString(KEY_TASK, "");
    }

    public void setTask(String task) {
        preferences.edit().putString(KEY_TASK, task == null ? "" : task).apply();
    }

    public String getNotes() {
        return preferences.getString(KEY_NOTES, "");
    }

    public void setNotes(String notes) {
        preferences.edit().putString(KEY_NOTES, notes == null ? "" : notes).apply();
    }

    public void reset() {
        preferences.edit().clear().apply();
    }

    public float progress() {
        return getPhase() / (float) (PHASES.length - 1);
    }

    public String phaseLabel() {
        return PHASES[getPhase()];
    }
}
