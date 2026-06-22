package de.maxhenkel.audioplayer.audioloader.importer;

import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.api.importer.ImportedAudio;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public class WebServerImporter implements AudioImporter {

    private final byte[] data;
    @Nullable
    private final String fileName;

    public WebServerImporter(byte[] data, @Nullable String fileName) {
        this.data = data;
        this.fileName = fileName;
    }

    @Override
    public AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception {
        return new AudioImportInfo(fileName);
    }

    @Override
    public byte[] onProcess(@Nullable ServerPlayer player) throws Exception {
        return data;
    }

    @Override
    public void onPostprocess(@Nullable ServerPlayer player, ImportedAudio importedAudio) throws Exception {

    }

    @Override
    public String getHandlerName() {
        return "webserver";
    }

}
