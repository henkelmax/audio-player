package de.maxhenkel.audioplayer.webserver;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.config.WebServerConfig;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public class WebServerEvents {

    public static WebServerConfig WEB_SERVER_CONFIG;

    @Nullable
    private static WebServer webServer;

    public static void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(WebServerEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(WebServerEvents::onServerStopped);

        if (AudioPlayerMod.SERVER_CONFIG.runWebServer.get()) {
            WEB_SERVER_CONFIG = ConfigBuilder.builder(WebServerConfig::new).path(AudioPlayerMod.getModConfigFolder().resolve("webserver.properties")).build();
        } else {
            WEB_SERVER_CONFIG = ConfigBuilder.builder(WebServerConfig::new).build();
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        closeServerIfRunning();
        if (!AudioPlayerMod.SERVER_CONFIG.runWebServer.get()) {
            return;
        }
        try {
            webServer = WebServer.create(server).start();
            AudioPlayerMod.LOGGER.info("Audio player upload web server started on port {}", webServer.getPort());
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to start web server", e);
        }
    }

    public static void onServerStopped(MinecraftServer server) {
        if (webServer != null) {
            AudioPlayerMod.LOGGER.info("Audio player upload web server stopped");
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
