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
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        mapper.register("/upload", new UploadHandler());
        mapper.register("*", new StaticContentHandler());

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
                player.sendSystemMessage(UploadCommands.sendUUIDMessage(token, Component.literal("Successfully uploaded sound."))); //TODO Rename sound to audio
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to upload a sound: {}", player.getName().getString(), e.getMessage());
                player.sendSystemMessage(Component.literal("Failed to upload sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

}
