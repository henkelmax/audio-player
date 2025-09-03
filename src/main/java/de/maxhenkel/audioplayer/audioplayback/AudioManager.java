package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.VoicechatAudioPlayerPlugin;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class AudioManager {

    @Nullable
    public static UUID playStationary(ServerLevel level, Vec3 pos, PlayerType type, AudioData sound, @Nullable Player player) {
        float range = sound.getRange(type);

        VoicechatServerApi api = VoicechatAudioPlayerPlugin.voicechatServerApi;
        if (api == null) {
            return null;
        }

        UUID soundIdToPlay = sound.getSoundIdToPlay();

        if (soundIdToPlay == null) {
            return null;
        }

        @Nullable UUID channelID = PlayerManager.instance().playLocational(
                api,
                level,
                pos,
                soundIdToPlay,
                (player instanceof ServerPlayer p) ? p : null,
                range,
                type.getCategory(),
                type.getMaxDuration().get()
        );

        return channelID;
    }

}
