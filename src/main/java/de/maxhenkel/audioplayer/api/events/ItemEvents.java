package de.maxhenkel.audioplayer.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ItemEvents {

    public static final Event<Consumer<ApplyEvent>> APPLY = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<ApplyEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public interface ApplyEvent {
        ItemStack getItemStack();
    }

}
