package de.maxhenkel.audioplayer;

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

public class ComponentUtils {

    public static final Holder<Instrument> EMPTY_INSTRUMENT = Holder.direct(new Instrument(Holder.direct(SoundEvents.EMPTY), 140, 256F, Component.empty()));

    public static final ResourceKey<JukeboxSong> CUSTOM_JUKEBOX_SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(AudioPlayer.MODID, "custom"));
    public static final JukeboxPlayable CUSTOM_JUKEBOX_PLAYABLE = new JukeboxPlayable(new EitherHolder<>(CUSTOM_JUKEBOX_SONG_KEY), false);

}
