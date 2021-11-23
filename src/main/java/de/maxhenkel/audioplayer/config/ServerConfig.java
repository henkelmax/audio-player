package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.ConfigEntry;

public class ServerConfig {

    public final ConfigEntry<String> filebinUrl;
    public final ConfigEntry<Integer> maxUploadSize;

    public ServerConfig(ConfigBuilder builder) {
        filebinUrl = builder.stringEntry("filebin_url", "https://filebin.net/");
        maxUploadSize = builder.integerEntry("max_upload_size", 1000 * 1000 * 50, 1, Integer.MAX_VALUE);
    }

}
