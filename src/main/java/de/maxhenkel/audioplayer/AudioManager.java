package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class AudioManager {

    @Nullable
    public static UUID play(ServerLevel level, BlockPos pos, PlayerType type, AudioData sound, @Nullable Player player) {
        float range = sound.getRange(type);

        VoicechatServerApi api = VoicechatAudioPlayerPlugin.voicechatServerApi;
        if (api == null) {
            return null;
        }

        UUID soundIdToPlay = sound.getSoundIdToPlay();

        if (soundIdToPlay == null) {
            return null;
        }

        @Nullable UUID channelID;
        if (type.equals(PlayerType.GOAT_HORN)) {
            Vec3 playerPos;
            if (player == null) {
                playerPos = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            } else {
                playerPos = player.position();
            }
            channelID = PlayerManager.instance().playLocational(
                    api,
                    level,
                    playerPos,
                    soundIdToPlay,
                    (player instanceof ServerPlayer p) ? p : null,
                    range,
                    type.getCategory(),
                    type.getMaxDuration().get()
            );
        } else {
            channelID = PlayerManager.instance().playLocational(
                    api,
                    level,
                    new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
                    soundIdToPlay,
                    (player instanceof ServerPlayer p) ? p : null,
                    range,
                    type.getCategory(),
                    type.getMaxDuration().get()
            );
        }

        return channelID;
    }

}
