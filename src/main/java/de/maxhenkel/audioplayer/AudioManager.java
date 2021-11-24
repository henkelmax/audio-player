package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.interfaces.IJukebox;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class AudioManager {

    public static LevelResource AUDIO_DATA = new LevelResource("audio_player_data");
    public static AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    public static short[] getSound(MinecraftServer server, UUID id) throws IOException, UnsupportedAudioFileException {
        Path soundFile = getSoundFile(server, id);

        if (!Files.exists(soundFile)) {
            throw new FileNotFoundException("Sound does not exist");
        }

        return readSound(soundFile);
    }

    public static short[] readSound(Path file) throws IOException, UnsupportedAudioFileException {
        AudioInputStream in = AudioSystem.getAudioInputStream(file.toFile());
        AudioInputStream convertedIn = AudioSystem.getAudioInputStream(FORMAT, in);
        return Plugin.voicechatApi.getAudioConverter().bytesToShorts(convertedIn.readAllBytes());
    }

    public static Path getSoundFile(MinecraftServer server, UUID id) {
        return server.getWorldPath(AUDIO_DATA).resolve(id.toString() + ".wav");
    }

    public static void saveSound(MinecraftServer server, UUID id, String url) throws UnsupportedAudioFileException, IOException {
        AudioInputStream in = AudioSystem.getAudioInputStream(new URL(url));
        Path soundFile = getSoundFile(server, id);
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AudioSystem.write(in, AudioFileFormat.Type.WAVE, baos);

        byte[] data = baos.toByteArray();

        if (data.length > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
            throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted((float) data.length / 1_000_000F, AudioPlayer.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F));
        }

        AudioSystem.write(AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)), AudioFileFormat.Type.WAVE, Files.newOutputStream(soundFile, StandardOpenOption.CREATE_NEW));
    }

    @Nullable
    public static UUID getCustomSound(ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.hasUUID("CustomSound")) {
            return null;
        }

        return tag.getUUID("CustomSound");
    }

    public static boolean playCustomMusicDisc(ServerLevel level, BlockPos pos, ItemStack musicDisc, @Nullable Player player) {
        UUID customSound = AudioManager.getCustomSound(musicDisc);

        if (customSound == null) {
            return false;
        }

        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) {
            return false;
        }

        @Nullable UUID channelID = PlayerManager.instance().play(api, level, pos, customSound, (player instanceof ServerPlayer p) ? p : null);

        if (level.getBlockEntity(pos) instanceof IJukebox jukebox) {
            jukebox.setChannelID(channelID);
        }

        return true;
    }

}
