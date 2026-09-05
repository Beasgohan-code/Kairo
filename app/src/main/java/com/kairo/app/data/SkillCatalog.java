package com.kairo.app.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Fixed, reviewable prompt skills. Skills shape answers; they never grant device or network access. */
public final class SkillCatalog {
    private static final List<SkillDefinition> SKILLS = Collections.unmodifiableList(Arrays.asList(
            new SkillDefinition("professional", "Professional answer", "Use a crisp structure, useful headings, and an actionable next step.",
                    "Prefer concise professional structure: lead with the result, use headings when helpful, state assumptions, and finish with a practical next step.", true),
            new SkillDefinition("code-review", "Code review", "Look for correctness, edge cases, security, and maintainability.",
                    "For code, check correctness, edge cases, error handling, security, performance, and maintainability. Call out risks instead of silently guessing.", true),
            new SkillDefinition("multi-language", "Polyglot coding", "Respect the selected JavaScript, TypeScript, Kotlin, Java, or shell preset.",
                    "Honor the selected language preset. Keep syntax, package conventions, filenames, and run instructions appropriate to that language; do not translate a request into another language without saying so.", true),
            new SkillDefinition("artifact", "Artifact-ready output", "Make generated files complete, named, and easy to review.",
                    "When a file is requested, provide a complete self-contained artifact, suggest a safe filename, and identify any dependencies or follow-up files. Never claim it was saved until the user confirms a save action.", true),
            new SkillDefinition("web-grounding", "Source grounding", "Separate verified live sources from reasoning and label uncertainty.",
                    "When live research is present, distinguish sourced facts from inference, include source URLs when available, and label changing availability, pricing, quotas, and free tiers as provider-dependent.", false),
            new SkillDefinition("image-analysis", "Image analysis", "Describe what is visible in user-provided images without inventing details.",
                    "If an image is attached and the model supports vision, inspect only visible evidence, state uncertainty, and do not infer sensitive attributes about people. If vision is unavailable, say so clearly.", true),
            new SkillDefinition("privacy", "Privacy guard", "Keep secrets local and avoid unsafe credential or personal-data handling.",
                    "Never ask the user to paste provider passwords, private keys, or one-time codes into chat. Recommend official browser or token setup and minimize personal data in examples.", true),
            new SkillDefinition("safe-actions", "Safe actions", "Keep external writes and phone actions explicit, reviewable, and user-confirmed.",
                    "Treat deployment, pushes, pull requests, workflow activation, webhooks, messages, and phone actions as review-first operations that require explicit user confirmation. Never perform root access, silent calls or messages, arbitrary shell commands, or background device control.", true)
    ));

    private SkillCatalog() { }

    public static List<SkillDefinition> all() { return SKILLS; }

    public static SkillDefinition find(String id) {
        if (id == null) return null;
        for (SkillDefinition skill : SKILLS) if (skill.getId().equals(id)) return skill;
        return null;
    }

    public static List<String> defaultIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (SkillDefinition skill : SKILLS) if (skill.isEnabledByDefault()) ids.add(skill.getId());
        return Arrays.asList(ids.toArray(new String[0]));
    }

    public static String instructions(List<String> enabledIds) {
        if (enabledIds == null || enabledIds.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (String id : enabledIds) {
            SkillDefinition skill = find(id);
            if (skill == null) continue;
            if (result.length() > 0) result.append("\n");
            result.append("- ").append(skill.getInstruction());
        }
        return result.toString();
    }
}
