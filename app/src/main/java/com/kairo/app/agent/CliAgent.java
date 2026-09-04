package com.kairo.app.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Executes only commands approved by CliCommandPolicy in a private working directory. */
public final class CliAgent {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String output);

        void onError(String message);
    }

    public void execute(String command, Callback callback) {
        execute(command, null, callback);
    }

    /** Runs a safe diagnostic with an optional private directory as its working directory. */
    public void execute(String command, File privateDirectory, Callback callback) {
        if (!CliCommandPolicy.isAllowed(command)) {
            callback.onError(CliCommandPolicy.rejectionReason(command));
            return;
        }
        EXECUTOR.execute(() -> {
            Process process = null;
            try {
                ProcessBuilder builder = new ProcessBuilder("/system/bin/sh", "-c", command.trim())
                        .redirectErrorStream(true);
                if (privateDirectory != null) {
                    if (!privateDirectory.exists() && !privateDirectory.mkdirs()) {
                        callback.onError("Kairo could not create its private sandbox directory.");
                        return;
                    }
                    builder.directory(privateDirectory);
                    // Keep diagnostics from accidentally reading a caller's shell profile.
                    builder.environment().put("HOME", privateDirectory.getAbsolutePath());
                    builder.environment().put("TMPDIR", privateDirectory.getAbsolutePath());
                }
                process = builder.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (output.length() < 12_000) output.append(line).append('\n');
                    }
                }
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    callback.onError("The command timed out after 10 seconds.");
                    return;
                }
                String result = output.toString().trim();
                if (result.isEmpty()) result = "(no output)";
                callback.onSuccess(result + "\n\nexit " + process.exitValue());
            } catch (Exception exception) {
                callback.onError(exception.getMessage() == null
                        ? "Could not run the command." : exception.getMessage());
            } finally {
                if (process != null) process.destroy();
            }
        });
    }
}
