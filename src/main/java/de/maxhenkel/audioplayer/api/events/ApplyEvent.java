package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import net.minecraft.world.item.ItemStack;

public interface ApplyEvent extends ModuleAccessor {

    ItemStack getItemStack();

}
