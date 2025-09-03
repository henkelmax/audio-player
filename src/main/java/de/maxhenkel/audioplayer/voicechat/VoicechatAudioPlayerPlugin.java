package de.maxhenkel.audioplayer.voicechat;

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

    public static String MUSIC_DISC_CATEGORY = "music_discs";
    public static String NOTE_BLOCK_CATEGORY = "note_blocks";
    public static String GOAT_HORN_CATEGORY = "goat_horns";

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
                .setId(MUSIC_DISC_CATEGORY)
                .setName("Music discs")
                .setDescription("The volume of all custom music discs")
                .setIcon(getIcon("category_music_discs.png"))
                .build();
        noteBlocks = voicechatServerApi.volumeCategoryBuilder()
                .setId(NOTE_BLOCK_CATEGORY)
                .setName("Note blocks")
                .setDescription("The volume of all note blocks with custom heads")
                .setIcon(getIcon("category_note_blocks.png"))
                .build();
        goatHorns = voicechatServerApi.volumeCategoryBuilder()
                .setId(GOAT_HORN_CATEGORY)
                .setName("Goat horns")
                .setDescription("The volume of all custom goat horns")
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
            e.printStackTrace();
        }
        return null;
    }

}
