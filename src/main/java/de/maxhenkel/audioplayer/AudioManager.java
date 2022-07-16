package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.interfaces.IJukebox;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.UUID;

public class AudioManager {

    public static LevelResource AUDIO_DATA = new LevelResource("audio_player_data");
    public static AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    public static final String MP3_EXTENSION = "mp3";
    public static final String WAV_EXTENSION = "wav";

    public static short[] getSound(MinecraftServer server, UUID id) throws IOException, UnsupportedAudioFileException {
        return readSound(getExistingSoundFile(server, id));
    }

    public static short[] readSound(Path file) throws IOException, UnsupportedAudioFileException {
        return Plugin.voicechatApi.getAudioConverter().bytesToShorts(convert(file, FORMAT));
    }

    public static Path getSoundFile(MinecraftServer server, UUID id, String extension) {
        return server.getWorldPath(AUDIO_DATA).resolve(id.toString() + "." + extension);
    }

    public static Path getExistingSoundFile(MinecraftServer server, UUID id) throws FileNotFoundException {
        Path file = getSoundFile(server, id, MP3_EXTENSION);
        if (Files.exists(file)) {
            return file;
        }
        file = getSoundFile(server, id, WAV_EXTENSION);
        if (Files.exists(file)) {
            return file;
        }
        throw new FileNotFoundException("Audio does not exist");
    }

    public static Path getUploadFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("audioplayer_uploads");
    }

    public static void saveSound(MinecraftServer server, UUID id, String url) throws UnsupportedAudioFileException, IOException {
        byte[] data = download(new URL(url), AudioPlayer.SERVER_CONFIG.maxUploadSize.get());

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));

        Path soundFile = getSoundFile(server, id, getExtension(audioInputStream.getFormat()));
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        try (OutputStream outputStream = Files.newOutputStream(soundFile)) {
            IOUtils.write(data, outputStream);
        }
    }

    public static void saveSound(MinecraftServer server, UUID id, Path file) throws UnsupportedAudioFileException, IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new NoSuchFileException("The file %s does not exist".formatted(file.toString()));
        }

        long size = Files.size(file);
        if (size > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
            throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted(Math.round((float) size / 1_000_000F), Math.round(AudioPlayer.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F)));
        }
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file.toFile());
        } finally {
            if (audioInputStream != null) {
                audioInputStream.close();
            }
        }

        Path soundFile = getSoundFile(server, id, getExtension(audioInputStream.getFormat()));
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        Files.move(file, soundFile);
    }

    public static String getExtension(AudioFormat format) throws UnsupportedAudioFileException {
        if (format.getEncoding().equals(MpegEncoding.MPEG1L3)) {
            return MP3_EXTENSION;
        } else if (
                format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) ||
                        format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED) ||
                        format.getEncoding().equals(AudioFormat.Encoding.PCM_FLOAT) ||
                        format.getEncoding().equals(AudioFormat.Encoding.ALAW) ||
                        format.getEncoding().equals(AudioFormat.Encoding.ULAW)
        ) {
            return WAV_EXTENSION;
        }
        throw new UnsupportedAudioFileException("Unsupported encoding: %s".formatted(format.getEncoding().toString()));
    }

    public static byte[] convert(Path file, AudioFormat audioFormat) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat sourceFormat = source.getFormat();
            AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
            AudioInputStream stream1 = AudioSystem.getAudioInputStream(convertFormat, source);
            AudioInputStream stream2 = AudioSystem.getAudioInputStream(audioFormat, stream1);
            return stream2.readAllBytes();
        }
    }

    private static byte[] download(URL url, int limit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(url.openStream());

        int nRead;
        byte[] data = new byte[32768];

        while ((nRead = bis.read(data, 0, data.length)) != -1) {
            bos.write(data, 0, nRead);
            if (bos.size() > limit) {
                bis.close();
                throw new IOException("Maximum file size of %sMB exceeded".formatted((int) (((float) limit) / 1_000_000F)));
            }
        }
        bis.close();
        return bos.toByteArray();
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

        @Nullable UUID channelID = PlayerManager.instance().playLocational(api, level, pos, customSound, (player instanceof ServerPlayer p) ? p : null);

        if (level.getBlockEntity(pos) instanceof IJukebox jukebox) {
            jukebox.setChannelID(channelID);
        }

        return true;
    }

    public static boolean playGoatHorn(ServerLevel level, ItemStack goatHorn, ServerPlayer player) {
        UUID customSound = AudioManager.getCustomSound(goatHorn);

        if (customSound == null) {
            return false;
        }

        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) {
            return false;
        }

        PlayerManager.instance().playGlobalRange(api, level, customSound, player, AudioPlayer.SERVER_CONFIG.goatHornRange.get().floatValue());
        return true;
    }

}
