package de.maxhenkel.audioplayer.api.data;

import javax.annotation.Nullable;

public interface ModuleAccessor {

    @Nullable
    <T extends AudioDataModule> T getModule(ModuleKey<T> moduleKey);

}
