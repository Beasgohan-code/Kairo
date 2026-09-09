package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.kairo.app.data.SkillDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** On-device store for user-authored skills. Not a secret vault — credentials are rejected. */
public final class CustomSkillStore {
    private static final String PREFS = "kairo_custom_skills";
    private static final String KEY = "skills_v1";
    private static final char RS = '\u001e';
    private static final char GS = '\u001f';

    private final SharedPreferences preferences;

    public CustomSkillStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<SkillDefinition> all() {
        return parse(preferences.getString(KEY, ""));
    }

    public SkillDefinition find(String id) {
        if (id == null) return null;
        for (SkillDefinition skill : all()) {
            if (id.equals(skill.getId())) return skill;
        }
        return null;
    }

    public int size() {
        return all().size();
    }

    public void save(SkillDefinition skill) {
        if (skill == null) throw new IllegalArgumentException("Skill is required.");
        SkillCreator.Draft draft = SkillCreator.fromFields(
                skill.getId(), skill.getName(), skill.getDescription(), skill.getInstruction());
        if (!draft.isOk()) throw new IllegalArgumentException(draft.getError());
        List<SkillDefinition> next = new ArrayList<>();
        boolean replaced = false;
        for (SkillDefinition existing : all()) {
            if (existing.getId().equals(draft.getSkill().getId())) {
                next.add(draft.getSkill());
                replaced = true;
            } else {
                next.add(existing);
            }
        }
        if (!replaced) {
            if (next.size() >= SkillCreator.MAX_CUSTOM) {
                throw new IllegalStateException("At most " + SkillCreator.MAX_CUSTOM
                        + " custom skills can be stored on this device.");
            }
            next.add(draft.getSkill());
        }
        preferences.edit().putString(KEY, serialize(next)).apply();
    }

    public void delete(String id) {
        if (id == null) return;
        List<SkillDefinition> next = new ArrayList<>();
        for (SkillDefinition existing : all()) {
            if (!id.equals(existing.getId())) next.add(existing);
        }
        preferences.edit().putString(KEY, serialize(next)).apply();
    }

    public static String serialize(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (SkillDefinition skill : skills) {
            if (skill == null) continue;
            if (out.length() > 0) out.append(GS);
            out.append(clean(skill.getId())).append(RS)
                    .append(clean(skill.getName())).append(RS)
                    .append(clean(skill.getDescription())).append(RS)
                    .append(clean(skill.getInstruction()));
        }
        return out.toString();
    }

    public static List<SkillDefinition> parse(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<SkillDefinition> skills = new ArrayList<>();
        String[] records = raw.split(String.valueOf(GS), -1);
        for (String record : records) {
            if (record == null || record.isEmpty()) continue;
            String[] parts = record.split(String.valueOf(RS), -1);
            if (parts.length < 4) continue;
            String id = parts[0];
            if (!SkillCreator.isCustomId(id)) continue;
            if (ApiKeyDetector.detect(parts[3]) != null) continue;
            skills.add(new SkillDefinition(id, parts[1], parts[2], parts[3], true));
        }
        return skills;
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replace(RS, ' ').replace(GS, ' ');
    }
}
