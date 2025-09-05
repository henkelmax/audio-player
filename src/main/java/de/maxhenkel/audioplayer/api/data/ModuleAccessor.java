package de.maxhenkel.audioplayer.api.data;

import java.util.Optional;

public interface ModuleAccessor {

    <T extends AudioDataModule> Optional<T> getModule(ModuleKey<T> moduleKey);

}
