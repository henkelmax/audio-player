package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.ConfigEntry;

public class ServerConfig {

    public final ConfigEntry<String> filebinUrl;
    public final ConfigEntry<Integer> maxUploadSize;
    public final ConfigEntry<Integer> uploadPermissionLevel;
    public final ConfigEntry<Integer> musicDiscPermissionLevel;
    public final ConfigEntry<Boolean> jukeboxHopperInteraction;
    public final ConfigEntry<Boolean> jukeboxDispenserInteraction;

    public ServerConfig(ConfigBuilder builder) {
        filebinUrl = builder.stringEntry("filebin_url", "https://filebin.net/");
        maxUploadSize = builder.integerEntry("max_upload_size", 1000 * 1000 * 50, 1, Integer.MAX_VALUE);
        uploadPermissionLevel = builder.integerEntry("upload_permission_level", 0, 0, Integer.MAX_VALUE);
        musicDiscPermissionLevel = builder.integerEntry("music_disc_permission_level", 0, 0, Integer.MAX_VALUE);
        jukeboxHopperInteraction = builder.booleanEntry("jukebox_hopper_interaction", true);
        jukeboxDispenserInteraction = builder.booleanEntry("jukebox_dispenser_interaction", true);
    }

}
