package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.ConfigEntry;

public class ServerConfig {

    public final ConfigEntry<String> filebinUrl;
    public final ConfigEntry<Integer> maxUploadSize;
    public final ConfigEntry<Integer> uploadPermissionLevel;
    public final ConfigEntry<Integer> applyToItemPermissionLevel;
    public final ConfigEntry<Integer> goatHornCooldown;
    public final ConfigEntry<Double> musicDiscRange;
    public final ConfigEntry<Double> goatHornRange;
    public final ConfigEntry<Boolean> allowWavUpload;
    public final ConfigEntry<Boolean> allowMp3Upload;
    public final ConfigEntry<Integer> maxMusicDiscDuration;
    public final ConfigEntry<Integer> maxGoatHornDuration;
    public final ConfigEntry<Integer> cacheSize;

    public ServerConfig(ConfigBuilder builder) {
        filebinUrl = builder.stringEntry("filebin_url", "https://filebin.net/");
        maxUploadSize = builder.integerEntry("max_upload_size", 1000 * 1000 * 20, 1, Integer.MAX_VALUE);
        uploadPermissionLevel = builder.integerEntry("upload_permission_level", 0, 0, Integer.MAX_VALUE);
        applyToItemPermissionLevel = builder.integerEntry("apply_to_item_permission_level", 0, 0, Integer.MAX_VALUE);
        goatHornCooldown = builder.integerEntry("goat_horn_cooldown", 140, 1, Short.MAX_VALUE);
        musicDiscRange = builder.doubleEntry("music_disc_range", 65D, 1D, Integer.MAX_VALUE);
        goatHornRange = builder.doubleEntry("goat_horn_range", 256D, 1D, Integer.MAX_VALUE);
        allowWavUpload = builder.booleanEntry("allow_wav_upload", true);
        allowMp3Upload = builder.booleanEntry("allow_mp3_upload", true);
        maxMusicDiscDuration = builder.integerEntry("max_music_disc_duration", 60 * 5, 1, Integer.MAX_VALUE);
        maxGoatHornDuration = builder.integerEntry("max_goat_horn_duration", 20, 1, Integer.MAX_VALUE);
        cacheSize = builder.integerEntry("cache_size", 16, 0, Integer.MAX_VALUE);
    }

}
