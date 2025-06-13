package de.maxhenkel.audioplayer.interfaces;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface CustomJukeboxSongPlayer {

    void audioplayer$onSave(ItemStack itemStack, ValueOutput valueOutput);

    void audioplayer$onLoad(ItemStack itemStack, ValueInput valueInput);

    boolean audioplayer$customPlay(ServerLevel level, ItemStack item);

    boolean audioplayer$customStop();

}
