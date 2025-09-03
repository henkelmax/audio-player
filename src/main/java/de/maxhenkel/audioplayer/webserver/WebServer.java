package de.maxhenkel.audioplayer.webserver;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.importer.WebServerImporter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.microhttp.*;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class WebServer implements AutoCloseable {

    protected final MinecraftServer minecraftServer;
    protected final TokenManager tokenManager;
    @Nullable
    protected EventLoop eventLoop;
    protected int port;
    @Nullable
    protected StaticFileCache staticFileCache;

    protected WebServer(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
        tokenManager = new TokenManager();
    }

    public static WebServer create(MinecraftServer minecraftServer) {
        return new WebServer(minecraftServer);
    }

    public WebServer start() throws Exception {
        port = WebServerEvents.WEB_SERVER_CONFIG.port.get();
        Options options = Options.builder()
                .withPort(port)
                .withHost(null)
                .withRequestTimeout(Duration.ofSeconds(60))
                .withConcurrency(1)
                .withMaxRequestSize(AudioPlayerMod.SERVER_CONFIG.maxUploadSize.get().intValue())
                .build();

        staticFileCache = StaticFileCache.of("web");

        eventLoop = new EventLoop(options, NoopLogger.instance(), this::handleRequest);
        eventLoop.start();

        return this;
    }

    private void handleRequest(Request request, Consumer<Response> responseConsumer) {
        if (!handleAuth(request, responseConsumer)) {
            return;
        }
        String path = request.uri();
        if (path.startsWith("/upload")) {
            handleUpload(request, responseConsumer);
        } else {
            handleServeStatic(request, responseConsumer);
        }
    }

    private boolean handleAuth(Request request, Consumer<Response> responseConsumer) {
        String username = WebServerEvents.WEB_SERVER_CONFIG.authUsername.get();
        String password = WebServerEvents.WEB_SERVER_CONFIG.authPassword.get();
        if (username.isBlank() || password.isBlank()) {
            return true;
        }
        String authHeader = request.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String encodedCredentials = authHeader.substring("Basic ".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(encodedCredentials));

            if (credentials.equals(username + ":" + password)) {
                return true;
            }
        }
        responseConsumer.accept(
                new Response(
                        401,
                        "UNAUTHORIZED",
                        List.of(
                                new Header("Content-Type", "text/plain"),
                                new Header("WWW-Authenticate", "Basic realm=\"Restricted Area\"")
                        ),
                        "Unauthorized\n".getBytes()
                )
        );
        return false;
    }

    private void handleUpload(Request request, Consumer<Response> responseConsumer) {
        List<Header> headers = new ArrayList<>();
        headers.add(new Header("Access-Control-Allow-Origin", "*"));
        headers.add(new Header("Access-Control-Allow-Methods", "*"));
        headers.add(new Header("Access-Control-Allow-Headers", "*"));
        if (request.method().equalsIgnoreCase("OPTIONS")) {
            responseConsumer.accept(
                    new Response(
                            204,
                            "NO CONTENT",
                            headers,
                            "".getBytes()
                    )
            );
            return;
        }
        if (!request.method().equalsIgnoreCase("POST")) {
            responseConsumer.accept(
                    new Response(
                            400,
                            "BAD REQUEST",
                            headers,
                            "Bad request".getBytes()
                    )
            );
            return;
        }
        String tokenValue = request.header("token");
        if (tokenValue == null) {
            responseConsumer.accept(
                    new Response(
                            401,
                            "UNAUTHORIZED",
                            headers,
                            "Unauthorized\n".getBytes()
                    )
            );
            return;
        }
        UUID token;
        try {
            token = UUID.fromString(tokenValue);
        } catch (IllegalArgumentException e) {
            responseConsumer.accept(
                    new Response(
                            400,
                            "BAD REQUEST",
                            headers,
                            "Bad request".getBytes()
                    )
            );
            return;
        }
        UUID playerId = tokenManager.useToken(token);
        if (playerId == null) {
            responseConsumer.accept(
                    new Response(
                            401,
                            "UNAUTHORIZED",
                            headers,
                            "Unauthorized\n".getBytes()
                    )
            );
            return;
        }
        byte[] data = request.body();

        if (data.length > AudioPlayerMod.SERVER_CONFIG.maxUploadSize.get()) {
            responseConsumer.accept(
                    new Response(
                            414,
                            "TOO LONG",
                            headers,
                            "Too long\n".getBytes()
                    )
            );
            return;
        }

        upload(playerId, token, data);
        responseConsumer.accept(
                new Response(
                        200,
                        "OK",
                        headers,
                        "".getBytes()
                )
        );
    }

    private void handleServeStatic(Request request, Consumer<Response> responseConsumer) {
        if (!request.method().equalsIgnoreCase("GET")) {
            responseConsumer.accept(
                    new Response(
                            400,
                            "BAD REQUEST",
                            List.of(),
                            "Bad Request\n".getBytes()
                    )
            );
            return;
        }
        String requestedResource = request.uri().split("\\?")[0];

        if (requestedResource.equals("/")) {
            requestedResource = "/index.html";
        }

        byte[] data = staticFileCache.get(requestedResource);

        if (data == null) {
            responseConsumer.accept(
                    new Response(
                            400,
                            "BAD REQUEST",
                            List.of(new Header("Content-Type", "text/plain")),
                            "Bad Request\n".getBytes()
                    )
            );
            return;
        }
        String mimeType = getMimeType(requestedResource);
        responseConsumer.accept(
                new Response(
                        200,
                        "OK",
                        mimeType != null ? List.of(new Header("Content-Type", "%s; charset=UTF-8".formatted(mimeType))) : List.of(),
                        data
                )
        );
    }

    /**
     * @return the port the webserver is running on or <code>-1</code> if not running
     */
    public int getPort() {
        return eventLoop != null ? port : -1;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    @Override
    public void close() {
        if (eventLoop != null) {
            eventLoop.stop();
            eventLoop = null;
        }
    }

    private static final Map<String, String> MIME_TYPES = Map.of(
            "html", "text/html",
            "css", "text/css",
            "js", "application/javascript",
            "ico", "image/x-icon"
    );

    @Nullable
    private static String getMimeType(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            path = path.substring(lastSlashIndex + 1);
        }
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return null;
        }
        String extension = path.substring(lastDotIndex + 1);
        return MIME_TYPES.get(extension);
    }

    private void upload(UUID playerId, UUID token, byte[] audioData) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        AudioStorageManager.instance().handleImport(new WebServerImporter(token, audioData, null), player); //TODO File name
    }

    @Nullable
    public static URI generateUploadUrl(UUID token) {
        String urlString = WebServerEvents.WEB_SERVER_CONFIG.url.get();

        if (urlString.isBlank()) {
            return null;
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            AudioPlayerMod.LOGGER.error("Invalid web server URL: {}", urlString);
            return null;
        }

        StringBuilder finalUrl = new StringBuilder();
        if (url.getProtocol() == null || url.getProtocol().isEmpty() || url.getProtocol().equals("http")) {
            finalUrl.append("http");
        } else if (url.getProtocol().equals("https")) {
            finalUrl.append("https");
        } else {
            AudioPlayerMod.LOGGER.error("Invalid web server URL protocol: {}", url.getProtocol());
            return null;
        }
        finalUrl.append("://");
        if (url.getHost().isEmpty()) {
            AudioPlayerMod.LOGGER.error("Invalid web server URL host: {}", url.getHost());
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
