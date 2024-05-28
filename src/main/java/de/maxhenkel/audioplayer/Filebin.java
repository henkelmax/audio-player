package de.maxhenkel.audioplayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class Filebin {

    public static final String USER_AGENT = "AudioPlayer/curl";

    public static void downloadSound(MinecraftServer server, UUID sound) throws IOException, InterruptedException, UnsupportedAudioFileException {
        String url = getBin(sound);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("%s responded with status %s".formatted(url, response.statusCode()));
            }

            JsonElement json = JsonParser.parseString(response.body());

            if (!(json instanceof JsonObject object)) {
                throw new IOException("Invalid response");
            }

            JsonElement filesElement = object.get("files");

            if (filesElement == null) {
                throw new IOException("No files uploaded");
            }

            if (!(filesElement instanceof JsonArray files)) {
                throw new IOException("No files uploaded");
            }

            for (JsonElement element : files) {
                if (!(element instanceof JsonObject file)) {
                    continue;
                }

                String contentType = file.get("content-type").getAsString();

                if (contentType.equals("audio/wav") || contentType.equals("audio/mpeg")) {
                    long size = file.get("bytes").getAsLong();

                    if (size > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
                        throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted(Math.round((float) size / 1_000_000F), Math.round(AudioPlayer.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F)));
                    }

                    String filename = file.get("filename").getAsString();
                    AudioManager.saveSound(server, sound, url + "/" + filename);
                    deleteBin(url);
                    return;
                }
            }
            throw new IOException("No mp3 or wav files uploaded");
        }
    }

    public static void deleteBin(String url) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("%s responded with status %s".formatted(url, response.statusCode()));
            }
        } catch (Exception e) {
            AudioPlayer.LOGGER.warn("Failed to delete bin '{}'", url, e);
        }
    }

    public static String getBin(UUID sound) {
        String filebinUrl = AudioPlayer.SERVER_CONFIG.filebinUrl.get();

        if (!filebinUrl.endsWith("/")) {
            filebinUrl += "/";
        }

        return filebinUrl + sound;
    }

}
