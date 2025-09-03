package de.maxhenkel.audioplayer.api.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public interface ModuleAccessor {

    @Nullable
    DataAccessor getModule(ResourceLocation id);

}
