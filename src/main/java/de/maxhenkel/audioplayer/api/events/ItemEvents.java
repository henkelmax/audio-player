package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
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

    public static final Event<Consumer<ClearEvent>> CLEAR = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<ClearEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public interface ClearEvent {
        ModuleAccessor getData();
        ItemStack getItemStack();
    }

}
