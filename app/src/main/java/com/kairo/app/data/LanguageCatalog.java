package com.kairo.app.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Supported artifact presets. The AI may still answer other formats when the user asks. */
public final class LanguageCatalog {
    private static final List<LanguagePreset> PRESETS = Collections.unmodifiableList(Arrays.asList(
            new LanguagePreset("auto", "Auto", "txt", "Let the request decide; preserve the model's suggested language."),
            new LanguagePreset("javascript", "JavaScript", "js", "Node.js or browser JavaScript."),
            new LanguagePreset("typescript", "TypeScript", "ts", "Typed JavaScript for Node.js or web applications."),
            new LanguagePreset("kotlin", "Kotlin", "kt", "Kotlin for Android or JVM projects."),
            new LanguagePreset("java", "Java", "java", "Java for Android or JVM projects."),
            new LanguagePreset("shell", "Linux shell", "sh", "Portable, non-root shell scripts and sandbox diagnostics."),
            new LanguagePreset("python", "Python", "py", "Python scripts and services."),
            new LanguagePreset("html", "HTML", "html", "Accessible document markup."),
            new LanguagePreset("css", "CSS", "css", "Web styling and layout."),
            new LanguagePreset("json", "JSON", "json", "Configuration or data artifact.")
    ));

    private LanguageCatalog() { }

    public static List<LanguagePreset> all() { return PRESETS; }

    public static LanguagePreset find(String id) {
        if (id == null) return PRESETS.get(0);
        for (LanguagePreset preset : PRESETS) if (preset.getId().equals(id)) return preset;
        return PRESETS.get(0);
    }
}
