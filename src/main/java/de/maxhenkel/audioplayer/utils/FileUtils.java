package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    public static String fileNameWithoutExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return name;
        }
        return name.substring(0, dotIndex);
    }

    @Nullable
    public static String sha256(InputStream stream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hash = digest.digest();

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            AudioPlayerMod.LOGGER.error("Failed to calculate SHA256 hash", e);
            return null;
        }
    }

    @Nullable
    public static String sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            AudioPlayerMod.LOGGER.error("Failed to calculate SHA256 hash", e);
            return null;
        }
    }

    public static String stripFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
    }

    private static final Pattern DUPLICATE_NAME_PATTERN = Pattern.compile("^(.*) \\((\\d+)\\)$");

    public static String deduplicateName(String name) {
        Matcher matcher = DUPLICATE_NAME_PATTERN.matcher(name);
        if (matcher.matches()) {
            return "%s (%d)".formatted(matcher.group(1), Integer.parseInt(matcher.group(2)) + 1);
        }
        return "%s (1)".formatted(name);
    }

    public static String fixName(String name) {
        return name.trim().replace("\"", "");
    }

}
