package com.kairo.app.agent;

import com.kairo.app.data.Artifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Best-effort, explicitly requested artifact runner. It never invokes a shell for script
 * languages, uses only Kairo's private cache directory, caps output/time, and reports when a
 * runtime is not installed. Running code is not a security boundary: users should run trusted code.
 */
public final class CodeRunner {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final ExecutorService OUTPUT_READER = Executors.newCachedThreadPool();
    private static final int MAX_SOURCE_CHARS = 500_000;
    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final long TIMEOUT_SECONDS = 10L;

    public interface Callback {
        void onSuccess(String output);
        void onError(String message);
    }

    /** Reports runtimes visible to this Android process without downloading or installing tools. */
    public String environmentReport() {
        String[][] runtimes = {
                {"Android shell", "sh"},
                {"Node.js", "node"},
                {"Deno", "deno"},
                {"Python 3", "python3"},
                {"Java", "java"},
                {"Java compiler", "javac"},
                {"Kotlin compiler", "kotlinc"}
        };
        StringBuilder result = new StringBuilder("Kairo private runner · Android Linux process\n");
        for (String[] runtime : runtimes) {
            result.append(runtime[0]).append(": ")
                    .append(findExecutable(runtime[1]) ? "available" : "not installed")
                    .append('\n');
        }
        result.append("Full Ubuntu/Docker is not bundled; shell artifacts remain syntax-only.");
        return result.toString().trim();
    }

    private boolean findExecutable(String command) {
        String path = System.getenv("PATH");
        if (path == null || path.trim().isEmpty()) path = "/system/bin:/system/xbin:/data/data/com.kairo.app/files/bin";
        for (String directory : path.split(File.pathSeparator)) {
            if (new File(directory, command).isFile()) return true;
        }
        return "sh".equals(command) && new File("/system/bin/sh").isFile();
    }

    public void run(Artifact artifact, File privateDirectory, Callback callback) {
        if (artifact == null) {
            callback.onError("Choose an artifact first.");
            return;
        }
        if (privateDirectory == null) {
            callback.onError("Kairo's private run directory is unavailable.");
            return;
        }
        String language = artifact.getLanguage() == null
                ? "" : artifact.getLanguage().trim().toLowerCase(Locale.US);
        if ("kotlin".equals(language) || "java".equals(language)) {
            // These paths are supported when a JVM/compiler is present, which is uncommon on
            // Android. The process failure below becomes a clear runtime-not-installed message.
        } else if (!("javascript".equals(language) || "typescript".equals(language)
                || "python".equals(language) || "shell".equals(language)
                || "bash".equals(language))) {
            callback.onError("Run/check is not available for " + artifact.getLanguage()
                    + ". The file can still be created, edited, exported, or shared.");
            return;
        }
        EXECUTOR.execute(() -> {
            File source = null;
            File runDirectory = null;
            try {
                if (artifact.getContent().length() > MAX_SOURCE_CHARS) {
                    throw new IllegalArgumentException("Run is limited to 500,000 source characters.");
                }
                if (!privateDirectory.exists() && !privateDirectory.mkdirs()) {
                    throw new IllegalStateException("Kairo could not open its private run directory.");
                }
                String suffix = suffixFor(language, artifact.getName());
                runDirectory = new File(privateDirectory, "kairo-run-" + UUID.randomUUID());
                if (!runDirectory.mkdirs()) {
                    throw new IllegalStateException("Kairo could not create a private run directory.");
                }
                source = new File(runDirectory, safeSourceName(artifact.getName(), suffix));
                try (FileOutputStream output = new FileOutputStream(source)) {
                    output.write(artifact.getContent().getBytes(StandardCharsets.UTF_8));
                }
                String result;
                if ("java".equals(language)) {
                    result = runJava(source, runDirectory, artifact.getName());
                } else if ("kotlin".equals(language)) {
                    result = runKotlin(source, runDirectory);
                } else if ("javascript".equals(language)) {
                    result = runProcess(Arrays.asList("node", source.getAbsolutePath()), runDirectory,
                            "Node.js is not installed in Kairo's app environment.");
                } else if ("typescript".equals(language)) {
                    result = runProcess(Arrays.asList("deno", "run", "--no-remote", "--no-npm",
                                    "--no-config", source.getAbsolutePath()), runDirectory,
                            "Deno is not installed in Kairo's app environment.");
                } else if ("python".equals(language)) {
                    result = runProcess(Arrays.asList("python3", "-I", source.getAbsolutePath()),
                            runDirectory, "Python 3 is not installed in Kairo's app environment.");
                } else {
                    // Shell is deliberately syntax-only. Arbitrary shell execution remains
                    // outside Kairo's safe CLI allow-list.
                    ProcessResult syntax = execute(Arrays.asList("/system/bin/sh", "-n", source.getAbsolutePath()),
                            runDirectory, "The Android shell is unavailable.");
                    result = syntax.exitCode == 0
                            ? "Shell syntax check passed. Kairo does not execute shell artifacts.\n\n" + syntax.output
                            : "Shell syntax check failed (exit " + syntax.exitCode + ")\n" + syntax.output;
                }
                callback.onSuccess(result);
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "The artifact could not be run." : message);
            } finally {
                if (source != null && source.exists()) source.delete();
                deleteTree(runDirectory);
            }
        });
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }

    private String safeSourceName(String name, String suffix) {
        String value = name == null ? "" : name.trim().replace('\\', '_').replace('/', '_');
        value = value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (value.isEmpty() || ".".equals(value) || "..".equals(value)) value = "main" + suffix;
        if (!value.toLowerCase(Locale.US).endsWith(suffix)) value += suffix;
        return value;
    }

    private String runJava(File source, File directory, String artifactName) throws Exception {
        String className = artifactName == null ? "" : artifactName.trim();
        int dot = className.lastIndexOf('.');
        if (dot > 0) className = className.substring(0, dot);
        if (!className.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("Java run/check needs a filename matching its public class name.");
        }
        ProcessResult compile = execute(Arrays.asList("javac", "-d", directory.getAbsolutePath(),
                source.getAbsolutePath()), directory, "A Java compiler is not installed in Kairo's app environment.");
        if (compile.exitCode != 0) return "Java compile failed (exit " + compile.exitCode + ")\n" + compile.output;
        ProcessResult run = execute(Arrays.asList("java", "-cp", directory.getAbsolutePath(), className),
                directory, "A JVM is not installed in Kairo's app environment.");
        return "Java compile passed.\n\n" + (run.exitCode == 0 ? run.output
                : "Java run failed (exit " + run.exitCode + ")\n" + run.output);
    }

    private String runKotlin(File source, File directory) throws Exception {
        File jar = new File(directory, "kairo-kotlin-" + UUID.randomUUID() + ".jar");
        try {
            ProcessResult compile = execute(Arrays.asList("kotlinc", source.getAbsolutePath(),
                    "-include-runtime", "-d", jar.getAbsolutePath()), directory,
                    "A Kotlin compiler is not installed in Kairo's app environment.");
            if (compile.exitCode != 0) return "Kotlin compile failed (exit " + compile.exitCode + ")\n" + compile.output;
            ProcessResult run = execute(Arrays.asList("java", "-jar", jar.getAbsolutePath()), directory,
                    "A JVM is not installed in Kairo's app environment.");
            return "Kotlin compile passed.\n\n" + (run.exitCode == 0 ? run.output
                    : "Kotlin run failed (exit " + run.exitCode + ")\n" + run.output);
        } finally {
            if (jar.exists()) jar.delete();
        }
    }

    private String runProcess(List<String> command, File directory, String unavailableMessage)
            throws Exception {
        ProcessResult result = execute(command, directory, unavailableMessage);
        if (result.exitCode != 0) {
            return "Process exited " + result.exitCode + "\n" + result.output;
        }
        return result.output.isEmpty() ? "Completed successfully (no output)." : result.output;
    }

    private ProcessResult execute(List<String> command, File directory, String unavailableMessage)
            throws Exception {
        Process process;
        try {
            process = new ProcessBuilder(new ArrayList<>(command))
                    .redirectErrorStream(true)
                    .directory(directory)
                    .start();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(unavailableMessage, exception);
        }
        Future<String> outputFuture = OUTPUT_READER.submit(() -> readOutput(process));
        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                throw new IllegalStateException("The run timed out after " + TIMEOUT_SECONDS + " seconds.");
            }
            String output;
            try {
                output = outputFuture.get(2, TimeUnit.SECONDS);
            } catch (Exception exception) {
                output = "(output unavailable)";
            }
            return new ProcessResult(process.exitValue(), output);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            try { process.getInputStream().close(); } catch (Exception ignored) { }
        }
    }

    private String readOutput(Process process) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < MAX_OUTPUT_CHARS) {
                    int remaining = MAX_OUTPUT_CHARS - output.length();
                    output.append(line, 0, Math.min(line.length(), remaining)).append('\n');
                }
            }
        }
        return output.toString().trim();
    }

    private String suffixFor(String language, String name) {
        if ("typescript".equals(language)) return ".ts";
        if ("javascript".equals(language)) return ".js";
        if ("python".equals(language)) return ".py";
        if ("java".equals(language)) return ".java";
        if ("kotlin".equals(language)) return ".kt";
        if ("shell".equals(language) || "bash".equals(language)) return ".sh";
        if (name != null) {
            String lower = name.toLowerCase(Locale.US);
            if (lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".py")
                    || lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".sh")) {
                return lower.substring(lower.lastIndexOf('.'));
            }
        }
        return ".sh";
    }

    private static final class ProcessResult {
        private final int exitCode;
        private final String output;

        private ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }
}
