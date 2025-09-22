package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import net.minecraft.world.item.ItemStack;

public interface ApplyEvent {

    ModuleAccessor getData();

    ItemStack getItemStack();

}
