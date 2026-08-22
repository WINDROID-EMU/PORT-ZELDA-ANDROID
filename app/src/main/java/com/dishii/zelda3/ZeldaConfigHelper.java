package com.dishii.zelda3;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to parse, read, modify, and save zelda3.ini configuration files
 * while preserving comments, ordering, and section headers.
 * Supports multiple storage locations (external /sdcard/zelda, app-specific data, internal storage).
 */
public class ZeldaConfigHelper {
    private static final String TAG = "ZeldaConfigHelper";
    public static final String INI_FILENAME = "zelda3.ini";

    private final Context context;
    private final List<String> rawLines = new ArrayList<>();

    public ZeldaConfigHelper(Context context) {
        this.context = context.getApplicationContext();
        load();
    }

    /**
     * Gets a list of all possible candidate config files across storage locations.
     */
    public static List<File> getCandidateConfigFiles(Context context) {
        List<File> files = new ArrayList<>();
        
        // 1. /sdcard/zelda/zelda3.ini
        try {
            File customDir = new File(Environment.getExternalStorageDirectory(), "zelda");
            if (!customDir.exists()) {
                customDir.mkdirs();
            }
            files.add(new File(customDir, INI_FILENAME));
        } catch (Exception ignored) {}

        // 2. /sdcard/Android/data/<package>/files/zelda3.ini (App-specific external storage)
        try {
            File appExternalDir = context.getExternalFilesDir(null);
            if (appExternalDir != null) {
                files.add(new File(appExternalDir, INI_FILENAME));
            }
        } catch (Exception ignored) {}

        // 3. /data/user/0/<package>/files/zelda3.ini (Internal app storage)
        try {
            File internalDir = context.getFilesDir();
            if (internalDir != null) {
                files.add(new File(internalDir, INI_FILENAME));
            }
        } catch (Exception ignored) {}

        return files;
    }

    public static File getConfigFile(Context context) {
        List<File> candidates = getCandidateConfigFiles(context);
        File bestCandidate = null;
        long latestMod = -1;

        for (File f : candidates) {
            if (f != null && f.exists() && f.length() > 0) {
                if (f.lastModified() > latestMod) {
                    latestMod = f.lastModified();
                    bestCandidate = f;
                }
            }
        }

        if (bestCandidate != null) {
            return bestCandidate;
        }

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Loads the INI file from disk. If none exists, loads default template from assets.
     */
    public synchronized void load() {
        rawLines.clear();
        File bestFile = getConfigFile(context);

        if (bestFile != null && bestFile.exists() && bestFile.length() > 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(bestFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    rawLines.add(line);
                }
                Log.i(TAG, "Loaded config from " + bestFile.getAbsolutePath() + " (" + rawLines.size() + " lines)");
                return;
            } catch (IOException e) {
                Log.e(TAG, "Error reading " + bestFile.getAbsolutePath(), e);
            }
        }

        // If no file exists or failed to read, initialize from default assets
        try (InputStream is = context.getAssets().open(INI_FILENAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rawLines.add(line);
            }
            Log.i(TAG, "Initialized default config from assets (" + rawLines.size() + " lines)");
        } catch (IOException e) {
            Log.e(TAG, "Error reading default assets zelda3.ini", e);
        }
    }

    /**
     * Returns the full content as a single string.
     */
    public synchronized String getRawText() {
        StringBuilder sb = new StringBuilder();
        for (String line : rawLines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Replaces the entire INI content with the provided text.
     */
    public synchronized void setRawText(String text) {
        rawLines.clear();
        if (text != null) {
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                rawLines.add(line);
            }
        }
    }

    /**
     * Gets a configuration value in a specific section.
     *
     * @param section Section name, e.g. "General", "Graphics", "Sound", "Features"
     * @param key     Key name, e.g. "Autosave", "ExtendedAspectRatio"
     * @param defVal  Default value if not found
     * @return Value as string (trimmed)
     */
    public synchronized String getValue(String section, String key, String defVal) {
        String currentSection = null;
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }
            if (currentSection != null && currentSection.equalsIgnoreCase(section)) {
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx != -1) {
                    String k = trimmed.substring(0, eqIdx).trim();
                    if (k.equalsIgnoreCase(key)) {
                        String val = trimmed.substring(eqIdx + 1).trim();
                        return val;
                    }
                }
            }
        }
        return defVal;
    }

    public synchronized boolean getBoolValue(String section, String key, boolean defVal) {
        String val = getValue(section, key, null);
        if (val == null) return defVal;
        int commentIdx = val.indexOf('#');
        if (commentIdx != -1) val = val.substring(0, commentIdx).trim();
        commentIdx = val.indexOf(';');
        if (commentIdx != -1) val = val.substring(0, commentIdx).trim();
        val = val.trim().toLowerCase();
        return val.equals("1") || val.equals("true") || val.equals("yes") || val.equals("on");
    }

    public synchronized int getIntValue(String section, String key, int defVal) {
        String val = getValue(section, key, null);
        if (val == null) return defVal;
        try {
            int commentIdx = val.indexOf('#');
            if (commentIdx != -1) val = val.substring(0, commentIdx).trim();
            commentIdx = val.indexOf(';');
            if (commentIdx != -1) val = val.substring(0, commentIdx).trim();
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    /**
     * Sets or updates a key-value pair in a specific section while preserving comments.
     */
    public synchronized void setValue(String section, String key, String value) {
        String targetHeader = "[" + section + "]";
        int sectionStartIdx = -1;
        int sectionEndIdx = rawLines.size();
        int keyLineIdx = -1;

        String currentSection = null;
        for (int i = 0; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (currentSection != null && currentSection.equalsIgnoreCase(section)) {
                    sectionEndIdx = i;
                    break;
                }
                currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                if (currentSection.equalsIgnoreCase(section)) {
                    sectionStartIdx = i;
                }
                continue;
            }

            if (currentSection != null && currentSection.equalsIgnoreCase(section)) {
                if (!trimmed.startsWith("#") && !trimmed.startsWith(";")) {
                    int eqIdx = trimmed.indexOf('=');
                    if (eqIdx != -1) {
                        String k = trimmed.substring(0, eqIdx).trim();
                        if (k.equalsIgnoreCase(key)) {
                            keyLineIdx = i;
                        }
                    }
                }
            }
        }

        String formattedLine = key + " = " + value;

        if (keyLineIdx != -1) {
            // Update existing line
            rawLines.set(keyLineIdx, formattedLine);
        } else if (sectionStartIdx != -1) {
            // Section exists, insert before sectionEndIdx
            rawLines.add(sectionEndIdx, formattedLine);
        } else {
            // Section doesn't exist, create it at the end
            rawLines.add("");
            rawLines.add(targetHeader);
            rawLines.add(formattedLine);
        }
    }

    /**
     * Removes a key from a section if it exists.
     */
    public synchronized void removeKey(String section, String key) {
        String currentSection = null;
        for (int i = 0; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }
            if (currentSection != null && currentSection.equalsIgnoreCase(section)) {
                if (!trimmed.startsWith("#") && !trimmed.startsWith(";")) {
                    int eqIdx = trimmed.indexOf('=');
                    if (eqIdx != -1) {
                        String k = trimmed.substring(0, eqIdx).trim();
                        if (k.equalsIgnoreCase(key)) {
                            rawLines.remove(i);
                            return;
                        }
                    }
                }
            }
        }
    }

    public synchronized void setBoolValue(String section, String key, boolean value) {
        setValue(section, key, value ? "1" : "0");
    }

    public synchronized void setIntValue(String section, String key, int value) {
        setValue(section, key, String.valueOf(value));
    }

    /**
     * Saves the current lines to ALL candidate INI files so all directories stay in sync.
     */
    public synchronized boolean save() {
        List<File> candidateFiles = getCandidateConfigFiles(context);
        boolean savedAny = false;

        for (File file : candidateFiles) {
            if (file == null) continue;
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    for (String line : rawLines) {
                        writer.write(line);
                        writer.write("\n");
                    }
                    writer.flush();
                }
                Log.i(TAG, "Saved config successfully to " + file.getAbsolutePath());
                savedAny = true;
            } catch (Exception e) {
                Log.w(TAG, "Could not write config to " + file.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        return savedAny;
    }

    /**
     * Restores default zelda3.ini from assets to all locations.
     */
    public synchronized boolean restoreDefaults(Context context) {
        try (InputStream is = context.getAssets().open(INI_FILENAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            rawLines.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                rawLines.add(line);
            }
            return save();
        } catch (IOException e) {
            Log.e(TAG, "Failed to restore default zelda3.ini from assets", e);
            return false;
        }
    }
}
