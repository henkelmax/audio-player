package de.maxhenkel.audioplayer.webserver;

import de.maxhenkel.audioplayer.AudioPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public class WebServerEvents {

    @Nullable
    private static WebServer webServer;

    public static void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(WebServerEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(WebServerEvents::onServerStopped);
    }

    public static void onServerStarted(MinecraftServer server) {
        closeServerIfRunning();
        if (!AudioPlayer.SERVER_CONFIG.runWebServer.get()) {
            return;
        }
        try {
            webServer = WebServer.create(server).start();
            AudioPlayer.LOGGER.info("Audio player upload web server started on port {}", webServer.getPort());
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to start web server", e);
        }
    }

    public static void onServerStopped(MinecraftServer server) {
        if (webServer != null) {
            AudioPlayer.LOGGER.info("Audio player upload web server stopped");
        }
        closeServerIfRunning();
    }

    private static void closeServerIfRunning() {
        if (webServer != null) {
            webServer.close();
            webServer = null;
        }
    }

    public static boolean isRunning() {
        return webServer != null;
    }

    @Nullable
    public static WebServer getWebServer() {
        return webServer;
    }

}
