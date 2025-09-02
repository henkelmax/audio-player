package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.InstrumentComponent;

public class ComponentUtils {

    public static final InstrumentComponent EMPTY_INSTRUMENT = new InstrumentComponent(Holder.direct(new Instrument(Holder.direct(SoundEvents.EMPTY), 140, 256F, Component.empty())));

    public static final ResourceKey<JukeboxSong> CUSTOM_JUKEBOX_SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(AudioPlayerMod.MODID, "custom"));
    public static final JukeboxPlayable CUSTOM_JUKEBOX_PLAYABLE = new JukeboxPlayable(new EitherHolder<>(CUSTOM_JUKEBOX_SONG_KEY));

}
