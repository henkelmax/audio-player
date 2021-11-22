package de.maxhenkel.audioplayer;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.audioplayer.command.AudioPlayerCommands;
import de.maxhenkel.audioplayer.config.ServerConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AudioPlayer implements ModInitializer {

    public static final String MODID = "audioplayer";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ServerConfig SERVER_CONFIG;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(AudioPlayerCommands::register);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER_CONFIG = ConfigBuilder.build(server.getServerDirectory().toPath().resolve("config").resolve(MODID).resolve("audioplayer-server.properties"), ServerConfig::new);
        });
    }
}
