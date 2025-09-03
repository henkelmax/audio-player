package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public class AudioEvents {

    public static final Event<Consumer<GetSoundIdEvent>> GET_SOUND_ID = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<GetSoundIdEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public interface GetSoundIdEvent extends ModuleAccessor {

        @Nullable
        UUID getSoundId();

        void setSoundId(@Nullable UUID soundId);
    }

}
