package de.maxhenkel.audioplayer.api.data;

import net.minecraft.resources.Identifier;

public interface ModuleKey<T extends AudioDataModule> {

    Identifier getId();

    T create();

}
