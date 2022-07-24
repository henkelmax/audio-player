package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final Map<UUID, de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> players;

    public PlayerManager() {
        this.players = new ConcurrentHashMap<>();
    }

    @Nullable
    public UUID playLocational(VoicechatServerApi api, ServerLevel level, Vec3 pos, UUID sound, @Nullable ServerPlayer p, float distance, String category) {
        UUID channelID = UUID.randomUUID();
        LocationalAudioChannel channel = api.createLocationalAudioChannel(channelID, api.fromServerLevel(level), api.createPosition(pos.x, pos.y, pos.z));
        if (channel == null) {
            return null;
        }
        channel.setCategory(category);
        channel.setDistance(distance);
        api.getPlayersInRange(api.fromServerLevel(level), channel.getLocation(), distance + 1F, serverPlayer -> {
            VoicechatConnection connection = api.getConnectionOf(serverPlayer);
            if (connection != null) {
                return connection.isDisabled();
            }
            return true;
        }).stream().map(Player::getPlayer).map(ServerPlayer.class::cast).forEach(player -> {
            player.displayClientMessage(Component.literal("You need to enable voice chat to hear custom audio"), true);
        });

        de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = playChannel(api, channel, level, sound, p);
        if (audioPlayer == null) {
            return null;
        }
        players.put(channelID, audioPlayer);
        return channelID;
    }

    @Nullable
    private de.maxhenkel.voicechat.api.audiochannel.AudioPlayer playChannel(VoicechatServerApi api, AudioChannel channel, ServerLevel level, UUID sound, ServerPlayer p) {
        try {
            de.maxhenkel.voicechat.api.audiochannel.AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), AudioManager.getSound(level.getServer(), sound));
            player.startPlaying();
            return player;
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to play audio: {}", e.getMessage());
            if (p != null) {
                p.displayClientMessage(Component.literal("Failed to play audio: %s".formatted(e.getMessage())).withStyle(ChatFormatting.DARK_RED), true);
            }
            return null;
        }
    }

    public void stop(UUID channelID) {
        de.maxhenkel.voicechat.api.audiochannel.AudioPlayer player = players.get(channelID);
        if (player != null) {
            player.stopPlaying();
        }
    }

    private static PlayerManager instance;

    public static PlayerManager instance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

}
