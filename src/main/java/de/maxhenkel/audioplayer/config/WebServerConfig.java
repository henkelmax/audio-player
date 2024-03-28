package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class WebServerConfig {

    public final ConfigEntry<Integer> port;
    public final ConfigEntry<String> url;
    //TODO Handle this in the webserver
    public final ConfigEntry<String> basePath;
    public final ConfigEntry<Long> tokenTimeout;
    //TODO
    // - Add basic auth
    // - Configurable timeout

    public WebServerConfig(ConfigBuilder builder) {
        port = builder.integerEntry(
                "port",
                8080,
                1,
                (int) Short.MAX_VALUE,
                "The webserver port"
        );
        url = builder.stringEntry(
                "url",
                "",
                "The URL under which the webserver is reachable",
                "Example: https://test.example.com",
                "If this is left empty, the user will be prompted to copy the token manually",
                "If its set, the link will be generated automatically and the user can just open a link"
        );
        basePath = builder.stringEntry(
                "base_path",
                "",
                "The base path under which the webserver is reachable",
                "Example: /audioplayer"
        );
        tokenTimeout = builder.longEntry(
                "token_timeout",
                1000L * 60L * 5L,
                "The timeout of the token in milliseconds"
        );
    }

}
