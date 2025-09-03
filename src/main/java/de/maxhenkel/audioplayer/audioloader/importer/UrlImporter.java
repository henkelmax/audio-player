package de.maxhenkel.audioplayer.audioloader.importer;

import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class UrlImporter implements AudioImporter {

    public static final String USER_AGENT = "AudioPlayer";

    private final UUID soundId;
    private final String urlString;
    private URL url;

    public UrlImporter(String url) {
        this.soundId = UUID.randomUUID();
        this.urlString = url;
    }

    @Override
    public AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception {
        url = new URI(urlString).toURL();
        return new AudioImportInfo(soundId, getFileNameFromUrl(url.toString()));
    }

    @Nullable
    public static String getFileNameFromUrl(String url) {
        String name = url.substring(url.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            return null;
        }
        return name;
    }

    @Override
    public byte[] onProcess(@Nullable ServerPlayer player) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.connect();
        try (InputStream is = connection.getInputStream()) {
            return is.readAllBytes();
        }
    }

    @Override
    public void onPostprocess(@Nullable ServerPlayer player) throws Exception {

    }

    @Override
    public String getHandlerName() {
        return "url";
    }

}
