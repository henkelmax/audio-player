package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import de.maxhenkel.audioplayer.api.events.ItemEvents;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.world.item.ItemStack;

public class ClearEventImpl implements ItemEvents.ClearEvent {

    private final AudioData audioData;
    private final ItemStack stack;

    public ClearEventImpl(AudioData audioData, ItemStack stack) {
        this.audioData = audioData;
        this.stack = stack;
    }

    @Override
    public ModuleAccessor getData() {
        return audioData;
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }

}
