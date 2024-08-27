package de.maxhenkel.audioplayer;

import de.maxhenkel.admiral.MinecraftAdmiral;
import de.maxhenkel.audioplayer.command.*;
import de.maxhenkel.audioplayer.config.ServerConfig;
import de.maxhenkel.audioplayer.config.WebServerConfig;
import de.maxhenkel.audioplayer.webserver.WebServerEvents;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AudioPlayer implements ModInitializer {

    public static final String MODID = "audioplayer";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ServerConfig SERVER_CONFIG;
    public static WebServerConfig WEB_SERVER_CONFIG;

    public static AudioCache AUDIO_CACHE;
    public static ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r, "AudioPlayerExecutor");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
            AudioPlayer.LOGGER.error("Uncaught exception in thread {}", t.getName(), e);
        });
        return thread;
    });

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MinecraftAdmiral.builder(dispatcher, registryAccess).addCommandClasses(
                    UploadCommands.class,
                    ApplyCommands.class,
                    UtilityCommands.class,
                    PlayCommands.class
            ).setPermissionManager(AudioPlayerPermissionManager.INSTANCE).build();
        });
        VolumeOverrideManager.init();
        FileNameManager.init();
        Path configFolder = FabricLoader.getInstance().getConfigDir().resolve(MODID);
        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new).path(configFolder.resolve("audioplayer-server.properties")).build();
        if (SERVER_CONFIG.runWebServer.get()) {
            WEB_SERVER_CONFIG = ConfigBuilder.builder(WebServerConfig::new).path(configFolder.resolve("webserver.properties")).build();
        } else {
            WEB_SERVER_CONFIG = ConfigBuilder.builder(WebServerConfig::new).build();
        }

        try {
            Files.createDirectories(AudioManager.getUploadFolder());
        } catch (IOException e) {
            LOGGER.warn("Failed to create upload folder", e);
        }

        AUDIO_CACHE = new AudioCache(SERVER_CONFIG.cacheSize.get());

        ServerLifecycleEvents.SERVER_STARTED.register(WebServerEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(WebServerEvents::onServerStopped);
    }
}
