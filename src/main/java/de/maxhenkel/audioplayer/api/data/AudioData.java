package de.maxhenkel.audioplayer.api.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface AudioData extends ModuleAccessor {

    <T extends AudioDataModule> void setModule(ModuleKey<T> moduleKey, T module);

    void saveToItem(ItemStack stack);

    void saveToItem(ItemStack stack, @Nullable Component lore);

}
