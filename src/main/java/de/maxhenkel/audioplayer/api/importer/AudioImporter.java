package de.maxhenkel.audioplayer.api.importer;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public interface AudioImporter {

    AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception;

    byte[] onProcess(@Nullable ServerPlayer player) throws Exception;

    void onPostprocess(@Nullable ServerPlayer player) throws Exception;

    String getHandlerName();

}
