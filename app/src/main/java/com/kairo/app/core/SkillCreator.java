package com.kairo.app.core;

import com.kairo.app.data.SkillDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles a short brief into a reviewable, permissionless skill.
 * Skills only shape wording. They never grant tools, network, or device access.
 */
public final class SkillCreator {
    public static final int MAX_BRIEF = 2_000;
    public static final int MAX_NAME = 48;
    public static final int MAX_DESCRIPTION = 160;
    public static final int MAX_INSTRUCTION = 1_600;
    public static final int MAX_CUSTOM = 24;
    public static final String ID_PREFIX = "user-";

    private static final Pattern LIKE_A_ROLE = Pattern.compile(
            "(?i)(?:answer|respond|act|write|review|teach|edit|speak)\\s+(?:like|as)\\s+(?:a|an|the)\\s+([^:]+)");
    private static final Pattern AS_A_ROLE = Pattern.compile(
            "(?i)\\b(?:like|as)\\s+(?:a|an|the)\\s+([^:.]+)");

    public static final class Draft {
        private final boolean ok;
        private final String error;
        private final SkillDefinition skill;
        private final String archetype;
        private final String card;

        private Draft(boolean ok, String error, SkillDefinition skill, String archetype, String card) {
            this.ok = ok;
            this.error = error == null ? "" : error;
            this.skill = skill;
            this.archetype = archetype == null ? "" : archetype;
            this.card = card == null ? "" : card;
        }

        public boolean isOk() {
            return ok;
        }

        public String getError() {
            return error;
        }

        public SkillDefinition getSkill() {
            return skill;
        }

        public String getArchetype() {
            return archetype;
        }

        public String getCard() {
            return card;
        }

        public String getArchetypeLabel() {
            return titleForArchetype(archetype);
        }
    }

    private SkillCreator() {
    }

    /** Curated briefs shown in the creator. Keep these professional — they are the product demo. */
    public static String[][] starters() {
        return new String[][]{
                {"Staff engineer",
                        "Answer like a staff Android engineer: trade-offs first, name the risk, no fake certainty."},
                {"Security review",
                        "Review like an application-security engineer: realistic exploit path, blast radius, then a concrete mitigation. Do not invent CVEs."},
                {"Incident lead",
                        "Respond as an incident commander: timeline, hypothesis, smallest check, blast radius. Do not declare root cause without evidence."},
                {"Code reviewer",
                        "Review diffs like a staff reviewer: blockers versus nits, quote the snippet, suggest a patch."},
                {"Teacher",
                        "Teach as a patient senior: start from the reader's current model, one analogy, then the precise version, then a check question."},
                {"Technical editor",
                        "Edit for a precise technical voice: cut filler, prefer specific verbs, keep every claim scoped."},
                {"PR author",
                        "Write pull-request titles and bodies in imperative mood. One intent. Risk and rollback when the change is not trivial."},
                {"Test designer",
                        "Propose the smallest tests that would fail today. Cover happy path, one edge, and one failure. Name the runner."}
        };
    }

    public static boolean isCustomId(String id) {
        if (id == null || !id.startsWith(ID_PREFIX)) return false;
        if (id.length() < ID_PREFIX.length() + 2 || id.length() > 56) return false;
        for (int i = ID_PREFIX.length(); i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '-') return false;
        }
        return !id.endsWith("-") && !id.contains("--");
    }

    public static Draft compile(String brief) {
        return compile(null, brief);
    }

    public static Draft compile(String requestedName, String brief) {
        if (brief == null || brief.trim().isEmpty()) {
            return fail("Describe the skill in a sentence or two. Example: “Answer like a staff Android engineer: trade-offs first, no fake certainty.”");
        }
        String raw = brief.trim();
        if (raw.length() > MAX_BRIEF) {
            return fail("Keep the brief under " + MAX_BRIEF + " characters.");
        }
        if (ApiKeyDetector.detect(raw) != null) {
            return fail("That brief looks like it contains a credential. Skills never store secrets — remove the key and try again.");
        }

        String archetype = detectArchetype(raw);
        String name = sanitizeName(requestedName != null && !requestedName.trim().isEmpty()
                ? requestedName.trim()
                : inferName(raw, archetype));
        if (name.isEmpty()) name = titleForArchetype(archetype);
        String id = ID_PREFIX + slug(name);
        if (!isCustomId(id)) id = ID_PREFIX + "skill-" + Integer.toHexString(Math.abs(name.hashCode()) % 0xffff);
        String description = inferDescription(raw, archetype);
        String instruction = buildInstruction(name, description, archetype, raw);
        if (instruction.length() > MAX_INSTRUCTION) {
            instruction = instruction.substring(0, MAX_INSTRUCTION - 1).trim() + "…";
        }
        SkillDefinition skill = new SkillDefinition(id, name, description, instruction, true);
        return new Draft(true, "", skill, archetype, formatCard(skill, archetype));
    }

    public static Draft fromFields(String id, String name, String description, String instruction) {
        String safeName = sanitizeName(name);
        if (safeName.isEmpty()) return fail("Give the skill a short name.");
        String safeId = id == null || id.trim().isEmpty() ? ID_PREFIX + slug(safeName) : id.trim().toLowerCase(Locale.US);
        if (!isCustomId(safeId)) {
            safeId = ID_PREFIX + slug(safeName);
            if (!isCustomId(safeId)) return fail("Could not build a safe skill id.");
        }
        if (instruction == null || instruction.trim().length() < 24) {
            return fail("Write a real operating procedure — at least a couple of sentences.");
        }
        if (ApiKeyDetector.detect(instruction) != null || ApiKeyDetector.detect(description) != null) {
            return fail("Remove credentials before saving. Skills are not a secret store.");
        }
        String desc = sanitizeLine(description, MAX_DESCRIPTION);
        if (desc.isEmpty()) desc = "User-authored skill that shapes Kairo’s answers.";
        String body = instruction.trim();
        if (!body.contains("does not grant") && !body.contains("only shapes")) {
            body = body + "\n\n" + constraints();
        }
        if (body.length() > MAX_INSTRUCTION) body = body.substring(0, MAX_INSTRUCTION).trim();
        SkillDefinition skill = new SkillDefinition(safeId, safeName, desc, body, true);
        return new Draft(true, "", skill, "custom", formatCard(skill, "custom"));
    }

    public static String formatCard(SkillDefinition skill, String archetype) {
        if (skill == null) return "";
        return "SKILL CARD\n"
                + "Name   ·  " + skill.getName() + "\n"
                + "Id     ·  " + skill.getId() + "\n"
                + "Kind   ·  " + titleForArchetype(archetype) + "\n\n"
                + skill.getDescription() + "\n\n"
                + "Wording only. This skill cannot run tools, write files, send network requests, or control the phone.";
    }

    public static String exportMarkdown(SkillDefinition skill) {
        if (skill == null) return "";
        return "# " + skill.getName() + "\n\n"
                + "`" + skill.getId() + "`  ·  custom skill  ·  wording only\n\n"
                + skill.getDescription() + "\n\n"
                + "## Operating procedure\n\n"
                + skill.getInstruction().trim() + "\n";
    }

    public static String slug(String name) {
        if (name == null) return "skill";
        StringBuilder out = new StringBuilder();
        boolean dash = false;
        String lower = name.toLowerCase(Locale.US);
        for (int i = 0; i < lower.length() && out.length() < 36; i++) {
            char c = lower.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                out.append(c);
                dash = false;
            } else if (!dash && out.length() > 0) {
                out.append('-');
                dash = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') out.deleteCharAt(out.length() - 1);
        return out.length() < 2 ? "skill" : out.toString();
    }

    private static Draft fail(String error) {
        return new Draft(false, error, null, "", "");
    }

    static String detectArchetype(String brief) {
        String lower = brief.toLowerCase(Locale.US);
        if (containsAny(lower, "security", "owasp", "injection", "xss", "secret", "threat model")) return "security";
        if (containsAny(lower, "unit test", "coverage", "tdd", "test the", "test designer")) return "testing";
        if (containsAny(lower, "code review", "pr review", "diff", "nitpick", "blockers versus")) return "review";
        if (containsAny(lower, "commit", "pull request", "pull-request", "changelog", "release note")) return "git";
        if (containsAny(lower, "incident", "debug", "root cause", "outage", "stack trace")) return "incident";
        if (containsAny(lower, "teach", "explain like", "eli5", "tutorial", "lesson")) return "teaching";
        if (containsAny(lower, "research", "compare vendors", "sources", "cite")) return "research";
        if (containsAny(lower, "prd", "product spec", "user story", "roadmap")) return "product";
        if (containsAny(lower, "android", "engineer", "kotlin", "java", "typescript", "refactor", "api", "code")) {
            return "engineering";
        }
        if (containsAny(lower, "write", "editor", "tone", "blog", "email", "copy", "voice")) return "writing";
        return "general";
    }

    static String titleForArchetype(String archetype) {
        if ("security".equals(archetype)) return "Security review";
        if ("testing".equals(archetype)) return "Test design";
        if ("review".equals(archetype)) return "Code review";
        if ("git".equals(archetype)) return "Change communication";
        if ("incident".equals(archetype)) return "Incident response";
        if ("teaching".equals(archetype)) return "Teaching";
        if ("research".equals(archetype)) return "Research";
        if ("product".equals(archetype)) return "Product writing";
        if ("writing".equals(archetype)) return "Editorial voice";
        if ("engineering".equals(archetype)) return "Engineering";
        if ("custom".equals(archetype)) return "Custom skill";
        return "Custom skill";
    }

    private static String inferName(String brief, String archetype) {
        String first = firstSentence(brief);
        first = first.replaceFirst("(?i)^(please\\s+|make (me )?a skill (that|to)\\s+|skill:?\\s+)", "");
        String role = roleFrom(first);
        if (role.length() >= 4 && role.length() <= MAX_NAME) return titleCase(role);
        if (first.length() > 42) {
            int cut = first.lastIndexOf(' ', 42);
            first = first.substring(0, cut > 16 ? cut : 42).trim();
        }
        if (first.length() < 4) return titleForArchetype(archetype);
        return titleCase(stripTrailingPunct(first));
    }

    private static String roleFrom(String sentence) {
        Matcher like = LIKE_A_ROLE.matcher(sentence);
        if (like.find()) return sanitizeName(stripTrailingPunct(like.group(1)));
        Matcher as = AS_A_ROLE.matcher(sentence);
        if (as.find()) return sanitizeName(stripTrailingPunct(as.group(1)));
        return "";
    }

    private static String stripTrailingPunct(String value) {
        if (value == null) return "";
        String compact = value.trim();
        while (!compact.isEmpty()) {
            char last = compact.charAt(compact.length() - 1);
            if (last == ':' || last == '.' || last == ',' || last == ';' || last == '-') {
                compact = compact.substring(0, compact.length() - 1).trim();
            } else {
                break;
            }
        }
        return compact;
    }

    private static String inferDescription(String brief, String archetype) {
        String first = firstSentence(brief);
        if (first.length() > MAX_DESCRIPTION) {
            int cut = first.lastIndexOf(' ', MAX_DESCRIPTION - 1);
            first = first.substring(0, cut > 40 ? cut : MAX_DESCRIPTION - 1).trim() + "…";
        }
        if (first.length() < 12) {
            return "Applies a " + titleForArchetype(archetype).toLowerCase(Locale.US)
                    + " posture to every answer until you disable it.";
        }
        return first;
    }

    private static String buildInstruction(String name, String description, String archetype, String brief) {
        StringBuilder out = new StringBuilder();
        out.append("You are applying the user-authored skill “").append(name).append("”.\n");
        out.append("Purpose: ").append(description).append("\n\n");
        out.append("When this skill is on:\n");
        out.append("- Apply it to every answer in this conversation unless the user explicitly pauses it.\n");
        out.append("- If the request is outside the skill, still be useful, then return to this posture.\n\n");
        out.append("Operating procedure:\n");
        for (String bullet : procedure(archetype, brief)) {
            out.append("- ").append(bullet).append('\n');
        }
        out.append('\n').append(outputShape(archetype)).append('\n');
        out.append('\n').append(qualityBar(archetype)).append('\n');
        out.append('\n').append(constraints());
        return out.toString().trim();
    }

    private static List<String> procedure(String archetype, String brief) {
        List<String> bullets = new ArrayList<>();
        String playbook = playbook(archetype);
        if (!playbook.isEmpty()) bullets.add(playbook);
        for (String line : splitBrief(brief)) {
            if (bullets.size() >= 8) break;
            String item = toImperative(line);
            if (item.length() < 8) continue;
            boolean dup = false;
            for (String existing : bullets) {
                if (existing.equalsIgnoreCase(item) || existing.toLowerCase(Locale.US).contains(item.toLowerCase(Locale.US))) {
                    dup = true;
                    break;
                }
            }
            if (!dup) bullets.add(item);
        }
        if (bullets.isEmpty()) {
            bullets.add("Follow the user’s brief faithfully, then state assumptions and a next step.");
        }
        return bullets;
    }

    private static String playbook(String archetype) {
        switch (archetype) {
            case "security":
                return "Lead with the realistic risk, then blast radius, then a concrete mitigation. Do not invent CVEs.";
            case "testing":
                return "Propose the smallest tests that would fail today. Cover happy path, one edge, and one failure.";
            case "review":
                return "Separate blockers from nits. Quote the offending snippet and suggest a patch, not a lecture.";
            case "git":
                return "Write in imperative mood. One intent per message. Mention risk and rollback when the change is not trivial.";
            case "incident":
                return "Timeline → hypothesis → smallest check → blast radius. Do not declare root cause without evidence.";
            case "teaching":
                return "Start from the reader’s current model. Use one analogy, then the precise version, then a check question.";
            case "research":
                return "Separate sourced facts from inference. Label uncertainty. Never invent URLs or paper titles.";
            case "product":
                return "Problem, user, success metric, non-goals, then the smallest shippable slice.";
            case "writing":
                return "Match the requested voice. Cut filler. Prefer specific verbs and concrete nouns.";
            case "engineering":
                return "State the approach, trade-offs, and a patch-ready next step. Call out what you did not verify.";
            default:
                return "Lead with the answer, then the why, then a practical next step.";
        }
    }

    private static String outputShape(String archetype) {
        String shape;
        switch (archetype) {
            case "security":
                shape = "Risk → blast radius → mitigation → residual uncertainty.";
                break;
            case "testing":
                shape = "Cases as a short list, then how to run them, then what would change your mind.";
                break;
            case "review":
                shape = "Blockers, then nits, then a suggested patch. No throat-clearing.";
                break;
            case "git":
                shape = "Title on one line, body with why / risk / rollback.";
                break;
            case "incident":
                shape = "Timeline, then hypotheses, then the next check only.";
                break;
            case "teaching":
                shape = "Analogy, precise version, then one check question.";
                break;
            default:
                shape = "Answer first, then the why, then one concrete next step.";
                break;
        }
        return "Output shape:\n- " + shape;
    }

    private static String qualityBar(String archetype) {
        String bar = "engineering".equals(archetype) || "security".equals(archetype) || "review".equals(archetype)
                ? "Prefer file paths, types, and numbers over adjectives. If you did not run it, say so."
                : "No fake certainty. Prefer specific nouns and verbs. If you are guessing, label it.";
        return "Quality bar:\n- " + bar;
    }

    private static String constraints() {
        return "Non-negotiable constraints:\n"
                + "- This skill only shapes wording and structure. It does not grant tools, network, phone, or file-write permissions.\n"
                + "- Never claim a file was saved, a command ran, or a message was sent without a tool result.\n"
                + "- Never request passwords, API keys, or one-time codes.\n"
                + "- If the request conflicts with safety or honesty, refuse the unsafe part and offer a safer alternative.";
    }

    private static List<String> splitBrief(String brief) {
        List<String> parts = new ArrayList<>();
        String normalized = brief.replace('\r', '\n');
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim().replaceFirst("^[-*•]\\s+", "");
            if (trimmed.isEmpty()) continue;
            if (trimmed.length() > 180) {
                for (String sentence : trimmed.split("(?<=[.!?])\\s+")) {
                    if (!sentence.trim().isEmpty()) parts.add(sentence.trim());
                }
            } else {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private static String toImperative(String line) {
        String value = line.trim();
        value = value.replaceFirst("(?i)^(please\\s+|i want you to\\s+|you should\\s+|always\\s+)", "");
        if (value.isEmpty()) return "";
        char first = value.charAt(0);
        if (Character.isLowerCase(first)) {
            value = Character.toUpperCase(first) + value.substring(1);
        }
        if (value.length() > 180) value = value.substring(0, 177).trim() + "…";
        return value;
    }

    private static String firstSentence(String brief) {
        String compact = brief.replaceAll("\\s+", " ").trim();
        int end = -1;
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                end = i;
                break;
            }
        }
        if (end > 12) return compact.substring(0, end + 1).trim();
        return compact;
    }

    private static String sanitizeName(String name) {
        if (name == null) return "";
        String compact = name.replaceAll("\\s+", " ").trim();
        compact = compact.replaceAll("[\\r\\n\\u001f\\u001e]", " ").trim();
        if (compact.length() > MAX_NAME) compact = compact.substring(0, MAX_NAME).trim();
        return compact;
    }

    private static String sanitizeLine(String value, int max) {
        if (value == null) return "";
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() > max) compact = compact.substring(0, max).trim();
        return compact;
    }

    private static String titleCase(String value) {
        String[] words = value.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            if (word.length() <= 3 && out.length() > 0) {
                out.append(word.toLowerCase(Locale.US));
            } else {
                out.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) out.append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static boolean containsAny(String hay, String... needles) {
        for (String needle : needles) {
            if (hay.contains(needle)) return true;
        }
        return false;
    }
}
