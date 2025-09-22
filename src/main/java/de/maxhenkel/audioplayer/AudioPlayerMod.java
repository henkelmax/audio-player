package de.maxhenkel.audioplayer;

import de.maxhenkel.admiral.MinecraftAdmiral;
import de.maxhenkel.audioplayer.api.AudioPlayerModule;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.command.*;
import de.maxhenkel.audioplayer.config.ServerConfig;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.webserver.WebServerEvents;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class AudioPlayerMod implements ModInitializer {

    public static final String MODID = "audioplayer";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ServerConfig SERVER_CONFIG;

    @Override
    public void onInitialize() {
        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new).path(getModConfigFolder().resolve("audioplayer-server.properties")).migration(ServerConfig::migrate).build();
        Lang.onInitialize();

        WebServerEvents.onInitialize();
        AudioStorageManager.onInitialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MinecraftAdmiral.builder(dispatcher, registryAccess).addCommandClasses(
                            UploadCommands.class,
                            ApplyCommands.class,
                            UtilityCommands.class,
                            VolumeCommands.class,
                            PlayCommands.class
                    ).setPermissionManager(AudioPlayerPermissionManager.INSTANCE)
                    .addArgumentTypes(registry -> {
                        registry.register(ServerFileArgument.class, new ServerFileArgument.ServerFileArgumentSupplier(), new ServerFileArgument.ServerFileArgumentTypeConverter());
                    })
                    .build();
        });

        AudioPlayerModule.onInitialize();
    }

    public static Path getModConfigFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve(MODID);
    }

}
