package de.maxhenkel.audioplayer.audioloader.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.audioplayer.utils.ComponentException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class FilebinImporter implements AudioImporter {

    public static final String USER_AGENT = "AudioPlayer/curl";

    private final UUID soundId;
    @Nullable
    private URL fileUrl;

    public FilebinImporter(UUID soundId) {
        this.soundId = soundId;
    }

    @Override
    public AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception {
        URI url = getBin(soundId);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(url)
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
                throw new ComponentException(Lang.translatable("audioplayer.no_files_uploaded"));
            }

            if (!(filesElement instanceof JsonArray files)) {
                throw new ComponentException(Lang.translatable("audioplayer.no_files_uploaded"));
            }

            for (JsonElement element : files) {
                if (!(element instanceof JsonObject file)) {
                    continue;
                }

                String contentType = file.get("content-type").getAsString();

                if (contentType.equals("audio/wav") || contentType.equals("audio/mpeg")) {
                    long size = file.get("bytes").getAsLong();
                    ChatUtils.checkFileSize(size);

                    String filename = file.get("filename").getAsString();
                    URI uri = new URI(url + "/" + new URI(null, null, filename, null).toASCIIString());
                    fileUrl = uri.toURL();
                    return new AudioImportInfo(soundId, filename);
                }
            }
            throw new ComponentException(Lang.translatable("audioplayer.no_valid_audio_files_uploaded"));
        }
    }

    @Override
    public byte[] onProcess(@Nullable ServerPlayer player) throws Exception {
        if (fileUrl == null) {
            throw new IOException("File URL not found");
        }
        HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
        connection.setRequestProperty("User-Agent", FilebinImporter.USER_AGENT);
        connection.connect();
        try (InputStream is = connection.getInputStream()) {
            return is.readAllBytes();
        }
    }

    @Override
    public void onPostprocess(@Nullable ServerPlayer player) throws Exception {
        if (fileUrl != null) {
            deleteBin(fileUrl.toURI());
        }
    }

    @Override
    public String getHandlerName() {
        return "filebin";
    }

    public static void deleteBin(URI url) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("%s responded with status %s".formatted(url, response.statusCode()));
            }
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.warn("Failed to delete bin '{}'", url, e);
        }
    }

    public static URI getBin(UUID sound) {
        String filebinUrl = AudioPlayerMod.SERVER_CONFIG.filebinUrl.get();

        if (!filebinUrl.endsWith("/")) {
            filebinUrl += "/";
        }

        return URI.create(filebinUrl + sound);
    }

    public static void sendFilebinUploadMessage(CommandSourceStack stack) {
        UUID uuid = UUID.randomUUID();
        URI uploadURL = getBin(uuid);

        MutableComponent msg = Lang.translatable("audioplayer.upload_filebin_instructions",
                Lang.translatable("audioplayer.this_link")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.OpenUrl(uploadURL))
                                    .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_open")));
                        })
                        .withStyle(ChatFormatting.GREEN),
                Component.literal("mp3").withStyle(ChatFormatting.GRAY),
                Component.literal("wav").withStyle(ChatFormatting.GRAY),
                Lang.translatable("audioplayer.here")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.RunCommand("/audioplayer filebin " + uuid))
                                    .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_confirm_upload")));
                        })
                        .withStyle(ChatFormatting.GREEN)
        );

        stack.sendSuccess(() -> msg, false);
    }

}
