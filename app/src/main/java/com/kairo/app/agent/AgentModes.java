package com.kairo.app.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for response-mode ids and picker labels.
 * Keeps the Chat mode chip, voice router, and slash commands from drifting apart.
 */
public final class AgentModes {
    public static final class Mode {
        private final String id;
        private final String shortLabel;
        private final String pickerLabel;

        private Mode(String id, String shortLabel, String pickerLabel) {
            this.id = id;
            this.shortLabel = shortLabel;
            this.pickerLabel = pickerLabel;
        }

        public String getId() {
            return id;
        }

        public String getShortLabel() {
            return shortLabel;
        }

        public String getPickerLabel() {
            return pickerLabel;
        }
    }

    private static final List<Mode> MODES = Collections.unmodifiableList(Arrays.asList(
            new Mode("chat", "Chat mode", "Chat mode · focused conversation"),
            new Mode("code", "Code agent", "Code agent · plan and review"),
            new Mode("hermes", "Hermes orchestrator", "Hermes orchestrator · plan and hand off"),
            new Mode("devloop", "Dev Loop", "Dev Loop · plan code test review"),
            new Mode("artifact", "Artifact agent", "Artifact agent · return complete files"),
            new Mode("browser", "Browser agent", "Browser agent · cite selected sources"),
            new Mode("research", "Research agent", "Research agent · compare options"),
            new Mode("automation", "Automation agent", "Automation agent · GitHub, Vercel, n8n"),
            new Mode("arena", "Arena agent", "Arena agent · critique answers"),
            new Mode("phone", "Safe phone", "Safe phone assistant · visible actions")
    ));

    private AgentModes() {
    }

    public static List<Mode> all() {
        return MODES;
    }

    public static String[] pickerLabels() {
        String[] labels = new String[MODES.size()];
        for (int i = 0; i < MODES.size(); i++) labels[i] = MODES.get(i).getPickerLabel();
        return labels;
    }

    public static String[] pickerIds() {
        String[] ids = new String[MODES.size()];
        for (int i = 0; i < MODES.size(); i++) ids[i] = MODES.get(i).getId();
        return ids;
    }

    public static boolean isKnown(String id) {
        return find(id) != null;
    }

    public static Mode find(String id) {
        if (id == null) return null;
        for (Mode mode : MODES) {
            if (mode.getId().equals(id)) return mode;
        }
        return null;
    }

    public static String labelFor(String id) {
        Mode mode = find(id);
        return mode == null ? "Chat mode" : mode.getShortLabel();
    }

    public static String normalize(String id) {
        return find(id) == null ? "chat" : id;
    }
}
