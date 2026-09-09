package com.kairo.app.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Supported artifact presets for create / AI / sandbox file templates. */
public final class LanguageCatalog {
    private static final List<LanguagePreset> PRESETS = Collections.unmodifiableList(Arrays.asList(
            new LanguagePreset("auto", "Auto", "txt", "Let the request decide; preserve the model's suggested language."),
            new LanguagePreset("javascript", "JavaScript", "js", "Node.js or browser JavaScript."),
            new LanguagePreset("typescript", "TypeScript", "ts", "Typed JavaScript for Node.js or web apps."),
            new LanguagePreset("kotlin", "Kotlin", "kt", "Kotlin for Android or JVM projects."),
            new LanguagePreset("java", "Java", "java", "Java for Android or JVM projects."),
            new LanguagePreset("cpp", "C++", "cpp", "High-performance C++ (C++17+) for native hot paths."),
            new LanguagePreset("c", "C", "c", "Portable C11 for low-level and systems work."),
            new LanguagePreset("assembly", "Assembly", "s", "Architecture-aware assembly notes (review-only on device)."),
            new LanguagePreset("python", "Python", "py", "Python scripts, tests, and services."),
            new LanguagePreset("css", "CSS", "css", "Web styling and design systems."),
            new LanguagePreset("html", "HTML", "html", "Accessible document markup."),
            new LanguagePreset("xml", "XML", "xml", "Android layouts, configs, and structured markup."),
            new LanguagePreset("json", "JSON", "json", "Configuration or data artifact."),
            new LanguagePreset("markdown", "Markdown", "md", "Docs, README, and notes."),
            new LanguagePreset("text", "Plain text", "txt", "Notes, logs, and freeform text."),
            new LanguagePreset("yaml", "YAML", "yml", "CI configs and structured settings."),
            new LanguagePreset("sql", "SQL", "sql", "Queries and schema sketches."),
            new LanguagePreset("go", "Go", "go", "Go services and CLI tools."),
            new LanguagePreset("rust", "Rust", "rs", "Safe systems Rust sketches."),
            new LanguagePreset("swift", "Swift", "swift", "Swift for Apple platforms."),
            new LanguagePreset("dart", "Dart", "dart", "Dart / Flutter UI code."),
            new LanguagePreset("shell", "Linux shell", "sh", "Portable non-root shell scripts and diagnostics."),
            new LanguagePreset("zip", "Zip bundle", "zip", "Package sandbox files into a reviewable archive (via sandbox zip).")
    ));

    private LanguageCatalog() { }

    public static List<LanguagePreset> all() { return PRESETS; }

    public static LanguagePreset find(String id) {
        if (id == null) return PRESETS.get(0);
        for (LanguagePreset preset : PRESETS) if (preset.getId().equals(id)) return preset;
        return PRESETS.get(0);
    }

    /** Starter body for quick file create templates. */
    public static String starterTemplate(String id) {
        if (id == null) id = "text";
        switch (id) {
            case "javascript":
                return "export function main() {\n  console.log('Hello from Kairo');\n}\n\nmain();\n";
            case "typescript":
                return "export function main(): void {\n  console.log('Hello from Kairo');\n}\n\nmain();\n";
            case "python":
                return "def main() -> None:\n    print('Hello from Kairo')\n\n\nif __name__ == '__main__':\n    main()\n";
            case "java":
                return "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from Kairo\");\n    }\n}\n";
            case "kotlin":
                return "fun main() {\n    println(\"Hello from Kairo\")\n}\n";
            case "cpp":
                return "#include <iostream>\n\nint main() {\n    std::cout << \"Hello from Kairo\\n\";\n    return 0;\n}\n";
            case "c":
                return "#include <stdio.h>\n\nint main(void) {\n    puts(\"Hello from Kairo\");\n    return 0;\n}\n";
            case "assembly":
                return "; Kairo assembly notes (review-only on device)\n; arch: aarch64 or x86_64 — fill in intentionally\n\n.global _start\n_start:\n    ; TODO\n\n";
            case "css":
                return ":root {\n  color-scheme: dark;\n  --bg: #07080c;\n  --fg: #f4f5fa;\n  --accent: #8b7cff;\n}\n\nbody {\n  margin: 0;\n  font-family: system-ui, sans-serif;\n  background: var(--bg);\n  color: var(--fg);\n}\n";
            case "html":
                return "<!doctype html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"utf-8\" />\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n  <title>Kairo</title>\n</head>\n<body>\n  <h1>Hello from Kairo</h1>\n</body>\n</html>\n";
            case "xml":
                return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <string name=\"app_name\">Kairo</string>\n</resources>\n";
            case "json":
                return "{\n  \"name\": \"kairo\",\n  \"version\": \"0.14.0\"\n}\n";
            case "markdown":
                return "# Kairo note\n\n- Idea\n- Next step\n\n";
            case "yaml":
                return "name: kairo\non: [push]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n";
            case "sql":
                return "CREATE TABLE notes (\n  id INTEGER PRIMARY KEY,\n  body TEXT NOT NULL\n);\n\nSELECT * FROM notes LIMIT 20;\n";
            case "go":
                return "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tfmt.Println(\"Hello from Kairo\")\n}\n";
            case "rust":
                return "fn main() {\n    println!(\"Hello from Kairo\");\n}\n";
            case "swift":
                return "import Foundation\n\nprint(\"Hello from Kairo\")\n";
            case "dart":
                return "void main() {\n  print('Hello from Kairo');\n}\n";
            case "shell":
                return "#!/usr/bin/env sh\nset -eu\necho \"Hello from Kairo\"\n";
            case "zip":
                return "# Use Sandbox → Zip to package private files into a .zip archive.\n# This placeholder is not a binary zip.\n";
            default:
                return "";
        }
    }
}
