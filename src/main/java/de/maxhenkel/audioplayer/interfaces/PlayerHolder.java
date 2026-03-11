package de.maxhenkel.audioplayer.interfaces;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public interface PlayerHolder {

    @Nullable
    ServerPlayer audioplayer$getPlayer();

    void audioplayer$setPlayer(@Nullable ServerPlayer player);

}
