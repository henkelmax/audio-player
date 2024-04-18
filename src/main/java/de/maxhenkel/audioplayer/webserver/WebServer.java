package de.maxhenkel.audioplayer.webserver;

import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.command.UploadCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class WebServer implements AutoCloseable {

    protected final MinecraftServer minecraftServer;
    protected final TokenManager tokenManager;
    @Nullable
    protected HttpServer server;
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
        SocketConfig socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();

        UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
        mapper.register("/upload", new BasicAuthHandler(new UploadHandler()));
        mapper.register("*", new BasicAuthHandler(new StaticContentHandler()));

        staticFileCache = StaticFileCache.of("web");

        server = ServerBootstrap.bootstrap()
                .setListenerPort(AudioPlayer.WEB_SERVER_CONFIG.port.get())
                .setSocketConfig(socketConfig)
                .setHandlerMapper(mapper)
                .create();

        server.start();

        return this;
    }

    /**
     * @return the port the webserver is running on or <code>-1</code> if not running
     */
    public int getPort() {
        return server != null ? server.getLocalPort() : -1;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }

    static class BasicAuthHandler implements HttpRequestHandler {

        private final HttpRequestHandler handler;

        public BasicAuthHandler(HttpRequestHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            String username = AudioPlayer.WEB_SERVER_CONFIG.authUsername.get();
            String password = AudioPlayer.WEB_SERVER_CONFIG.authPassword.get();
            if (username.isBlank() || password.isBlank()) {
                handler.handle(request, response, context);
                return;
            }
            Header authHeader = request.getFirstHeader("Authorization");
            if (authHeader != null && authHeader.getValue().startsWith("Basic ")) {
                String encodedCredentials = authHeader.getValue().substring("Basic ".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(encodedCredentials));

                if (credentials.equals(username + ":" + password)) {
                    handler.handle(request, response, context);
                    return;
                }
            }

            response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Restricted Area\"");
            response.setEntity(new StringEntity("Unauthorized", ContentType.TEXT_PLAIN));
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

    private class StaticContentHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            if (!request.getRequestLine().getMethod().equalsIgnoreCase("GET")) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            String requestedResource = request.getRequestLine().getUri().split("\\?")[0];

            if (requestedResource.equals("/")) {
                requestedResource = "/index.html";
            }

            byte[] data = staticFileCache.get(requestedResource);

            if (data == null) {
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                return;
            }
            String mimeType = getMimeType(requestedResource);
            if (mimeType != null) {
                response.setHeader("Content-Type", "%s; charset=UTF-8".formatted(mimeType));
            }
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(new ByteArrayEntity(data));
        }
    }

    private class UploadHandler implements HttpRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            if (request.getRequestLine().getMethod().equalsIgnoreCase("OPTIONS")) {
                response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                return;
            }
            if (!request.getRequestLine().getMethod().equalsIgnoreCase("POST")) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            Header tokenHeader = request.getFirstHeader("token");
            if (tokenHeader == null) {
                response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
                return;
            }
            String tokenValue = tokenHeader.getValue();
            UUID token;
            try {
                token = UUID.fromString(tokenValue);
            } catch (IllegalArgumentException e) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            UUID playerId = tokenManager.useToken(token);
            if (playerId == null) {
                response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
                return;
            }
            if (!(request instanceof HttpEntityEnclosingRequest enclosingRequest)) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            HttpEntity entity = enclosingRequest.getEntity();

            if (entity.getContentLength() > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
                response.setStatusCode(HttpStatus.SC_REQUEST_TOO_LONG);
                return;
            }

            InputStream inputStream = entity.getContent();

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            byte[] bytes = bis.readAllBytes();
            upload(playerId, token, bytes);
            response.setStatusCode(HttpStatus.SC_OK);
        }
    }

    private void upload(UUID playerId, UUID token, byte[] audioData) {
        ServerPlayer player = minecraftServer.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        new Thread(() -> {
            try {
                AudioManager.saveSound(minecraftServer, token, null, audioData); //TODO File name
                player.sendSystemMessage(UploadCommands.sendUUIDMessage(token, Component.literal("Successfully uploaded sound.")));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to upload a sound: {}", player.getName().getString(), e.getMessage());
                player.sendSystemMessage(Component.literal("Failed to upload sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

}
