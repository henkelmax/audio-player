package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.events.ItemEvents;
import net.minecraft.world.item.ItemStack;

public class ApplyEventImpl implements ItemEvents.ApplyEvent {

    private final ItemStack stack;

    public ApplyEventImpl(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }
}
