package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class WebServerConfig {

    public final ConfigEntry<Integer> port;
    public final ConfigEntry<String> url;
    public final ConfigEntry<Long> tokenTimeout;
    public final ConfigEntry<String> authUsername;
    public final ConfigEntry<String> authPassword;
    //TODO Configurable timeout

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
        tokenTimeout = builder.longEntry(
                "token_timeout",
                1000L * 60L * 5L,
                "The timeout of the token in milliseconds"
        );
        authUsername = builder.stringEntry(
                "auth_username",
                "",
                "The username for basic auth",
                "If this is left empty, no auth will be used"
        );
        authPassword = builder.stringEntry(
                "auth_password",
                "",
                "The password for basic auth",
                "If this is left empty, no auth will be used"
        );
    }

}
