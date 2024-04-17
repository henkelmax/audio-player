package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class ServerConfig {

    public final ConfigEntry<String> filebinUrl;
    public final ConfigEntry<Long> maxUploadSize;
    public final ConfigEntry<Integer> goatHornCooldown;
    public final ConfigEntry<Float> musicDiscRange;
    public final ConfigEntry<Float> noteBlockRange;
    public final ConfigEntry<Float> goatHornRange;
    public final ConfigEntry<Float> maxGoatHornRange;
    public final ConfigEntry<Float> maxNoteBlockRange;
    public final ConfigEntry<Float> maxMusicDiscRange;
    public final ConfigEntry<Boolean> allowWavUpload;
    public final ConfigEntry<Boolean> allowMp3Upload;
    public final ConfigEntry<Integer> maxMusicDiscDuration;
    public final ConfigEntry<Integer> maxNoteBlockDuration;
    public final ConfigEntry<Integer> maxGoatHornDuration;
    public final ConfigEntry<Integer> cacheSize;
    public final ConfigEntry<Boolean> allowStaticAudio;
    public final ConfigEntry<Boolean> runWebServer;

    public ServerConfig(ConfigBuilder builder) {
        filebinUrl = builder.stringEntry(
                "filebin_url",
                "https://filebin.net/",
                "The URL of the Filebin service that the mod should use"
        );
        maxUploadSize = builder.longEntry(
                "max_upload_size",
                1000L * 1000L * 20L,
                1L,
                (long) Integer.MAX_VALUE,
                "The maximum allowed size of an uploaded file in bytes"
        );
        goatHornCooldown = builder.integerEntry(
                "goat_horn_cooldown",
                140,
                1,
                (int) Short.MAX_VALUE,
                "The cooldown of goat horns with custom audio in ticks"
        );
        musicDiscRange = builder.floatEntry(
                "music_disc_range",
                65F,
                1F,
                (float) Integer.MAX_VALUE,
                "The range of music discs with custom audio in blocks"
        );
        noteBlockRange = builder.floatEntry(
                "note_block_range",
                16F,
                1F,
                (float) Integer.MAX_VALUE,
                "The range of note blocks with custom audio in blocks"
        );
        goatHornRange = builder.floatEntry(
                "goat_horn_range",
                256F,
                1F,
                (float) Integer.MAX_VALUE,
                "The range of goat horns with custom audio in blocks"
        );
        maxMusicDiscRange = builder.floatEntry(
                "max_music_disc_range",
                256F,
                1F,
                (float) Integer.MAX_VALUE,
                "The maximum allowed range of a music disc with custom audio in blocks"
        );
        maxNoteBlockRange = builder.floatEntry(
                "max_note_block_range",
                256F,
                1F,
                (float) Integer.MAX_VALUE,
                "The maximum allowed range of a note block with custom audio in blocks"
        );
        maxGoatHornRange = builder.floatEntry(
                "max_goat_horn_range",
                512F,
                1F,
                (float) Integer.MAX_VALUE,
                "The maximum allowed range of a goat horn with custom audio in blocks"
        );
        allowWavUpload = builder.booleanEntry(
                "allow_wav_upload",
                true,
                "Whether users should be able to upload .wav files",
                "Note that .wav files are not compressed and can be very large",
                "Playing .wav files may result in more RAM usage"
        );
        allowMp3Upload = builder.booleanEntry(
                "allow_mp3_upload",
                true,
                "Whether users should be able to upload .mp3 files",
                "Note that .mp3 files require Simple Voice Chats mp3 decoder",
                "Playing .mp3 files can be slightly more CPU intensive"
        );
        maxMusicDiscDuration = builder.integerEntry(
                "max_music_disc_duration",
                60 * 5,
                1,
                Integer.MAX_VALUE,
                "The maximum allowed duration of a custom music disc in seconds"
        );
        maxNoteBlockDuration = builder.integerEntry(
                "max_note_block_duration",
                60 * 5,
                1,
                Integer.MAX_VALUE,
                "The maximum allowed duration of a note block with custom audio in seconds"
        );
        maxGoatHornDuration = builder.integerEntry(
                "max_goat_horn_duration",
                20,
                1,
                Integer.MAX_VALUE,
                "The maximum allowed duration of a custom goat horn in seconds"
        );
        cacheSize = builder.integerEntry(
                "cache_size",
                16,
                0,
                Integer.MAX_VALUE,
                "The maximum amount of audio files that are cached in memory",
                "Setting this to 0 will disable the cache",
                "A higher value will result in less disk reads, but more RAM usage"
        );
        allowStaticAudio = builder.booleanEntry(
                "allow_static_audio",
                true,
                "Static audio does not have directionality or falloff (volume does not decrease with distance)",
                "The /audioplayer setstatic [enabled] command can be used when this is set to true",
                "If this config option is disabled, static audio is completely disabled and will play as if the option wouldn't be set"
        );
        runWebServer = builder.booleanEntry(
                "run_web_server",
                false,
                "If the mod should run a webserver for uploads",
                "You can configure the webserver in the webserver.properties config",
                "The webserver.properties will only be generated if this option is set to true",
                "NOTE: This option is experimental and subject to change"
        );
    }

}
