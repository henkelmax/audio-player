package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.ApplyEvent;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ApplyEventImpl implements ApplyEvent {

    private final AudioData audioData;
    private final ItemStack stack;

    public ApplyEventImpl(AudioData audioData, ItemStack stack) {
        this.audioData = audioData;
        this.stack = stack;
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }

    @Override
    @Nullable
    public <T extends AudioDataModule> T getModule(ModuleKey<T> moduleKey) {
        return audioData.getModule(moduleKey).orElse(null);
    }
}
