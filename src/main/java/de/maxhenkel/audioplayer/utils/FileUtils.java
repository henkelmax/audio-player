package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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

}
