package de.maxhenkel.audioplayer.webserver;

import de.maxhenkel.audioplayer.AudioPlayer;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class UrlUtils {

    @Nullable
    public static URI generateUploadUrl(UUID token) {
        String urlString = AudioPlayer.WEB_SERVER_CONFIG.url.get();

        if (urlString.isBlank()) {
            return null;
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            AudioPlayer.LOGGER.error("Invalid web server URL: {}", urlString);
            return null;
        }

        StringBuilder finalUrl = new StringBuilder();
        if (url.getProtocol() == null || url.getProtocol().isEmpty() || url.getProtocol().equals("http")) {
            finalUrl.append("http");
        } else if (url.getProtocol().equals("https")) {
            finalUrl.append("https");
        } else {
            AudioPlayer.LOGGER.error("Invalid web server URL protocol: {}", url.getProtocol());
            return null;
        }
        finalUrl.append("://");
        if (url.getHost().isEmpty()) {
            AudioPlayer.LOGGER.error("Invalid web server URL host: {}", url.getHost());
            return null;
        }
        finalUrl.append(url.getHost());
        if (url.getPort() != -1) {
            finalUrl.append(":");
            finalUrl.append(url.getPort());
        }

        finalUrl.append("?token=");
        finalUrl.append(token.toString());

        return URI.create(finalUrl.toString());
    }

}
