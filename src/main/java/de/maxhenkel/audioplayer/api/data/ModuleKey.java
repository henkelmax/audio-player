package de.maxhenkel.audioplayer.api.data;

import net.minecraft.resources.ResourceLocation;

public interface ModuleKey<T extends AudioDataModule> {

    ResourceLocation getId();

    T create();

}
