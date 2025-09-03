package de.maxhenkel.audioplayer.audioloader.importer;

import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

public class WebServerImporter implements AudioImporter {

    private final UUID soundId;
    private final byte[] data;
    @Nullable
    private final String fileName;

    public WebServerImporter(UUID soundId, byte[] data, @Nullable String fileName) {
        this.soundId = soundId;
        this.data = data;
        this.fileName = fileName;
    }

    @Override
    public AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception {
        return new AudioImportInfo(soundId, fileName);
    }

    @Override
    public byte[] onProcess(@Nullable ServerPlayer player) throws Exception {
        return data;
    }

    @Override
    public void onPostprocess(@Nullable ServerPlayer player) throws Exception {

    }

    @Override
    public String getHandlerName() {
        return "webserver";
    }

}
