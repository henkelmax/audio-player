package de.maxhenkel.audioplayer.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.Consumer;

public class AudioEvents {

    public static final Event<Consumer<PlayEvent>> PLAY_MUSIC_DISC = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<PlayEvent> listener : listeners) {
            listener.accept(event);
            if (event.isCancelled()) {
                break;
            }
        }
    });

    public static final Event<Consumer<PostPlayEvent>> POST_PLAY_MUSIC_DISC = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<PostPlayEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public static final Event<Consumer<PlayEvent>> PLAY_GOAT_HORN = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<PlayEvent> listener : listeners) {
            listener.accept(event);
            if (event.isCancelled()) {
                break;
            }
        }
    });

    public static final Event<Consumer<PostPlayEvent>> POST_PLAY_GOAT_HORN = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<PostPlayEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public static final Event<Consumer<PlayEvent>> PLAY_NOTE_BLOCK = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<PlayEvent> listener : listeners) {
            listener.accept(event);
            if (event.isCancelled()) {
                break;
            }
        }
    });

    public static final Event<Consumer<PostPlayEvent>> POST_PLAY_NOTE_BLOCK = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<PostPlayEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public static final Event<Consumer<GetSoundIdEvent>> GET_SOUND_ID = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<GetSoundIdEvent> listener : listeners) {
            listener.accept(event);
        }
    });

    public static final Event<Consumer<GetDistanceEvent>> GET_DISTANCE = EventFactory.createArrayBacked(Consumer.class, listeners -> event -> {
        for (Consumer<GetDistanceEvent> listener : listeners) {
            listener.accept(event);
        }
    });

}
