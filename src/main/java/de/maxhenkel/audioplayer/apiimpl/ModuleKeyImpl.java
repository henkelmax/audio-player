package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class ModuleKeyImpl<T extends AudioDataModule> implements ModuleKey<T> {

    protected Identifier id;
    protected Supplier<T> constructor;

    public ModuleKeyImpl(Identifier id, Supplier<T> constructor) {
        this.id = id;
        this.constructor = constructor;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public T create() {
        return constructor.get();
    }

}
