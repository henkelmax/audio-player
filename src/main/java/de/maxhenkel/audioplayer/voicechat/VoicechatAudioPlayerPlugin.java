package de.maxhenkel.audioplayer.voicechat;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.AudioPlayerConstants;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Enumeration;

public class VoicechatAudioPlayerPlugin implements VoicechatPlugin {

    public static VoicechatApi voicechatApi;
    @Nullable
    public static VoicechatServerApi voicechatServerApi;
    @Nullable
    public static VolumeCategory musicDiscs;
    @Nullable
    public static VolumeCategory noteBlocks;
    @Nullable
    public static VolumeCategory goatHorns;

    @Override
    public String getPluginId() {
        return "audioplayer";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        voicechatServerApi = event.getVoicechat();
        musicDiscs = voicechatServerApi.volumeCategoryBuilder()
                .setId(AudioPlayerConstants.MUSIC_DISC_CATEGORY)
                .setName("Music discs")
                .setNameTranslationKey("audioplayer.category.music_discs")
                .setDescription("The volume of all custom music discs")
                .setDescriptionTranslationKey("audioplayer.category.music_discs.description")
                .setIcon(getIcon("category_music_discs.png"))
                .build();
        noteBlocks = voicechatServerApi.volumeCategoryBuilder()
                .setId(AudioPlayerConstants.NOTE_BLOCK_CATEGORY)
                .setName("Note blocks")
                .setNameTranslationKey("audioplayer.category.note_blocks")
                .setDescription("The volume of all note blocks with custom heads")
                .setDescriptionTranslationKey("audioplayer.category.note_blocks.description")
                .setIcon(getIcon("category_note_blocks.png"))
                .build();
        goatHorns = voicechatServerApi.volumeCategoryBuilder()
                .setId(AudioPlayerConstants.GOAT_HORN_CATEGORY)
                .setName("Goat horns")
                .setNameTranslationKey("audioplayer.category.goat_horns")
                .setDescription("The volume of all custom goat horns")
                .setDescriptionTranslationKey("audioplayer.category.goat_horns.description")
                .setIcon(getIcon("category_goat_horns.png"))
                .build();

        voicechatServerApi.registerVolumeCategory(musicDiscs);
        voicechatServerApi.registerVolumeCategory(noteBlocks);
        voicechatServerApi.registerVolumeCategory(goatHorns);
    }

    @Nullable
    private int[][] getIcon(String path) {
        try {
            Enumeration<URL> resources = VoicechatAudioPlayerPlugin.class.getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                BufferedImage bufferedImage = ImageIO.read(resources.nextElement().openStream());
                if (bufferedImage.getWidth() != 16) {
                    continue;
                }
                if (bufferedImage.getHeight() != 16) {
                    continue;
                }
                int[][] image = new int[16][16];
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    for (int y = 0; y < bufferedImage.getHeight(); y++) {
                        image[x][y] = bufferedImage.getRGB(x, y);
                    }
                }
                return image;
            }

        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to load icon '{}'", path, e);
        }
        return null;
    }

}
