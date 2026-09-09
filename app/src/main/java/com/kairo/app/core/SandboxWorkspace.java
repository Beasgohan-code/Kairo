package com.kairo.app.core;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Private on-phone workspace under the app's internal storage ({@link Context#getFilesDir()}).
 * Only this app can read it (Android app sandbox). Not a full Ubuntu VM —
 * bounded create / list / read / zip only.
 */
public final class SandboxWorkspace {
    private static final String ROOT = "kairo_sandbox";
    private static final long MAX_FILE_BYTES = 1_000_000;
    private static final int MAX_FILES = 120;

    private final File root;

    public SandboxWorkspace(Context context) {
        root = new File(context.getFilesDir(), ROOT);
        ensureDir(root);
        ensureDir(new File(root, "src"));
        ensureDir(new File(root, "tests"));
        ensureDir(new File(root, "out"));
        ensureDir(new File(root, "notes"));
    }

    public File getRoot() {
        return root;
    }

    /** Absolute path on this phone (app-private). */
    public String storageLocation() {
        return root.getAbsolutePath();
    }

    public List<String> listFiles() {
        List<String> names = new ArrayList<>();
        listRecursive(root, root, names);
        return names;
    }

    public String readText(String relativePath) throws IOException {
        File file = resolve(relativePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found in sandbox: " + relativePath);
        }
        if (file.length() > MAX_FILE_BYTES) {
            throw new IOException("File exceeds sandbox read limit");
        }
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int read = in.read(data);
            if (read < 0) read = 0;
            return new String(data, 0, read, StandardCharsets.UTF_8);
        }
    }

    public File writeText(String relativePath, String content) throws IOException {
        if (countFiles(root) >= MAX_FILES) {
            throw new IOException("Sandbox file limit reached (" + MAX_FILES + ")");
        }
        byte[] data = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_FILE_BYTES) {
            throw new IOException("Content exceeds " + MAX_FILE_BYTES + " bytes");
        }
        File out = resolve(relativePath);
        File parent = out.getParentFile();
        if (parent != null) ensureDir(parent);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(data);
        }
        return out;
    }

    public boolean delete(String relativePath) {
        File file = resolve(relativePath);
        return file.exists() && file.isFile() && file.delete();
    }

    public File zipAll(String zipName) throws IOException {
        String safeZip = safeSegment(zipName.endsWith(".zip") ? zipName : zipName + ".zip");
        File zipFile = new File(root, safeZip);
        List<File> files = new ArrayList<>();
        collectFiles(root, files);
        if (files.isEmpty()) throw new IOException("Sandbox is empty");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            byte[] buffer = new byte[8192];
            for (File f : files) {
                if (f.getName().equals(safeZip)) continue;
                String entryName = root.toURI().relativize(f.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(entryName));
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
        return zipFile;
    }

    public String statusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Phone private storage (app-only)\n");
        sb.append(storageLocation()).append('\n');
        sb.append("Folders: src/  tests/  out/  notes/\n");
        sb.append("Limits: ").append(MAX_FILES).append(" files · ")
                .append(MAX_FILE_BYTES).append(" bytes/file\n");
        sb.append("Not a full Ubuntu VM — bounded workspace only.\n\n");
        List<String> files = listFiles();
        if (files.isEmpty()) {
            sb.append("(empty — write files under src/ or notes/)\n");
        } else {
            for (String line : files) sb.append(" • ").append(line).append('\n');
        }
        return sb.toString();
    }

    private void listRecursive(File base, File dir, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                listRecursive(base, f, out);
            } else if (f.isFile()) {
                String rel = base.toURI().relativize(f.toURI()).getPath();
                out.add(rel + "  (" + f.length() + " B)");
            }
        }
    }

    private void collectFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) collectFiles(f, out);
            else if (f.isFile()) out.add(f);
        }
    }

    private int countFiles(File dir) {
        int n = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) n += countFiles(f);
            else if (f.isFile()) n++;
        }
        return n;
    }

    private File resolve(String relativePath) {
        String cleaned = relativePath == null ? "untitled.txt" : relativePath.trim().replace('\\', '/');
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        StringBuilder safe = new StringBuilder();
        for (String part : cleaned.split("/")) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) continue;
            if (safe.length() > 0) safe.append('/');
            safe.append(safeSegment(part));
        }
        if (safe.length() == 0) safe.append("untitled.txt");
        return new File(root, safe.toString());
    }

    private static String safeSegment(String name) {
        String cleaned = name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64);
        if (cleaned.startsWith(".")) cleaned = "f" + cleaned;
        return cleaned.isEmpty() ? "file.txt" : cleaned;
    }

    private static void ensureDir(File dir) {
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }
}
