package de.maxhenkel.audioplayer.api.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface AudioData extends ModuleAccessor {

    /**
     *
     * @param moduleKey the module key
     * @param module    the module to set
     * @param <T>       the module key type
     * @throws IllegalArgumentException if the module is <code>null</code>
     */
    <T extends AudioDataModule> void setModule(ModuleKey<T> moduleKey, T module);

    /**
     *
     * @param moduleKey the module key to remove
     * @param <T>       the module key type
     * @return the removed module or <code>null</code> if the module did not exist
     * @throws IllegalArgumentException if the module key is the base audio player module
     */
    @Nullable
    <T extends AudioDataModule> T removeModule(ModuleKey<T> moduleKey);

    void saveToItem(ItemStack stack);

    void saveToItem(ItemStack stack, @Nullable Component lore);

}
