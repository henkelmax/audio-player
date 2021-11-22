package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public UUID play(VoicechatServerApi api, ServerLevel level, BlockPos pos, UUID sound) {
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
            player.displayClientMessage(new TextComponent("You need to enable voice chat to hear this music disc"), true);
        });

        try {
            de.maxhenkel.voicechat.api.audiochannel.AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), AudioManager.getSound(level.getServer(), sound));
            players.put(channelID, player);
            player.startPlaying();
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to start audio player: {}", e.getMessage());
            return null;
        }
        return channelID;
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
