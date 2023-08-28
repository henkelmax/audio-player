package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.command.AudioPlayerCommands;
import de.maxhenkel.audioplayer.config.ServerConfig;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AudioPlayer implements ModInitializer {

    public static final String MODID = "audioplayer";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ServerConfig SERVER_CONFIG;

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
        CommandRegistrationCallback.EVENT.register(AudioPlayerCommands::register);

        SERVER_CONFIG = ConfigBuilder.build(FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("audioplayer-server.properties"), ServerConfig::new);

        try {
            Files.createDirectories(AudioManager.getUploadFolder());
        } catch (IOException e) {
            LOGGER.warn("Failed to create upload folder", e);
        }

        AUDIO_CACHE = new AudioCache(SERVER_CONFIG.cacheSize.get());
    }
}
