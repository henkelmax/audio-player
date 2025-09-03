package de.maxhenkel.audioplayer.api.data;

import de.maxhenkel.audioplayer.api.AudioPlayerModule;

import javax.annotation.Nullable;

public interface ModuleAccessor {

    @Nullable
    <T extends AudioPlayerModule> T getModule(ModuleKey<T> moduleKey);

}
