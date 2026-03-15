package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.InstrumentComponent;

import java.util.Optional;

public class ComponentUtils {

    public static final InstrumentComponent EMPTY_INSTRUMENT = new InstrumentComponent(Holder.direct(new Instrument(Holder.direct(SoundEvents.EMPTY), 140, 256F, Component.empty())));

    public static final ResourceKey<JukeboxSong> CUSTOM_JUKEBOX_SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(AudioPlayerMod.MODID, "custom"));
    private static JukeboxPlayable customPlayable;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Registry<JukeboxSong> jukeboxRegistry = server.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
            Holder.Reference<JukeboxSong> customJukeboxSong = jukeboxRegistry.get(CUSTOM_JUKEBOX_SONG_KEY).orElseThrow(() -> new IllegalStateException("Custom jukebox song not found"));
            customPlayable = new JukeboxPlayable(customJukeboxSong);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            customPlayable = null;
        });
    }

    public static Optional<JukeboxPlayable> getCustomPlayable() {
        return Optional.ofNullable(customPlayable);
    }

}
