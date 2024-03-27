package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class WebServerConfig {

    public final ConfigEntry<Integer> port;
    //TODO
    // - Add basic auth
    // - Add base path
    // - Configurable timeout
    // - Own domain that's shown in-game

    public WebServerConfig(ConfigBuilder builder) {
        port = builder.integerEntry(
                "port",
                8080,
                1,
                (int) Short.MAX_VALUE,
                "The webserver port"
        );
    }

}
