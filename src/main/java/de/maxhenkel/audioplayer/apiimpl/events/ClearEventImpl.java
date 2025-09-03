package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.events.ItemEvents;
import net.minecraft.world.item.ItemStack;

public class ClearEventImpl implements ItemEvents.ClearEvent {

    private final ItemStack stack;

    public ClearEventImpl(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }
}
