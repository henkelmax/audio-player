package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final Map<UUID, de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> players;

    public PlayerManager() {
        this.players = new ConcurrentHashMap<>();
    }

    @Nullable
    public UUID playLocational(VoicechatServerApi api, ServerLevel level, BlockPos pos, UUID sound, @Nullable ServerPlayer p) {
        UUID channelID = UUID.randomUUID();
        LocationalAudioChannel channel = api.createLocationalAudioChannel(channelID, api.fromServerLevel(level), api.createPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
        if (channel == null) {
            return null;
        }
        api.getPlayersInRange(api.fromServerLevel(level), channel.getLocation(), api.getBroadcastRange(), serverPlayer -> {
            VoicechatConnection connection = api.getConnectionOf(serverPlayer);
            if (connection != null) {
                return connection.isDisabled();
            }
            return true;
        }).stream().map(Player::getPlayer).map(ServerPlayer.class::cast).forEach(player -> {
            player.displayClientMessage(Component.literal("You need to enable voice chat to hear this music disc"), true);
        });

        de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = playChannel(api, channel, level, sound, p);
        if (audioPlayer == null) {
            return null;
        }
        players.put(channelID, audioPlayer);
        return channelID;
    }

    public void playGlobalRange(VoicechatServerApi api, ServerLevel level, UUID sound, ServerPlayer p, float range) {
        List<ServerPlayer> nearbyPlayers = level.getPlayers(p1 -> p1.distanceTo(p) <= range);

        for (ServerPlayer player : nearbyPlayers) {
            playStaticToPlayer(api, level, sound, player);
        }
    }

    public void playStaticToPlayer(VoicechatServerApi api, ServerLevel level, UUID sound, ServerPlayer p) {
        UUID channelID = UUID.randomUUID();
        VoicechatConnection connection = api.getConnectionOf(p.getUUID());
        if (connection == null) {
            return;
        }
        if (connection.isDisabled()) {
            p.displayClientMessage(Component.literal("You need to enable voice chat to hear goat horns"), true);
        }

        StaticAudioChannel channel = api.createStaticAudioChannel(channelID, api.fromServerLevel(level), connection);
        if (channel == null) {
            return;
        }

        playChannel(api, channel, level, sound, p);
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
