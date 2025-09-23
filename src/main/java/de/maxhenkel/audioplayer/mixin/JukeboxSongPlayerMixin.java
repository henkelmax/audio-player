package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.events.AudioEvents;
import de.maxhenkel.audioplayer.audioplayback.PlayerManager;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.interfaces.CustomJukeboxSongPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerMixin implements CustomJukeboxSongPlayer {

    @Shadow
    @Nullable
    private Holder<JukeboxSong> song;
    @Shadow
    @Final
    private JukeboxSongPlayer.OnSongChanged onSongChanged;

    @Shadow
    private long ticksSinceSongStarted;

    @Shadow
    @Final
    private BlockPos blockPos;

    @Unique
    @Nullable
    private UUID channelId;

    @Override
    public boolean audioplayer$customPlay(ServerLevel level, ItemStack item) {
        AudioData data = AudioData.of(item);
        if (data == null) {
            return false;
        }
        ChannelReference<?> channel = PlayerManager.instance().playType(level, null, data, PlayerType.MUSIC_DISC, AudioEvents.PLAY_MUSIC_DISC, AudioEvents.POST_PLAY_MUSIC_DISC, blockPos.getCenter());
        if (channel == null) {
            return false;
        }
        channelId = channel.getChannel().getId();
        song = null;
        ticksSinceSongStarted = 0L;
        onSongChanged.notifyChange();
        return true;
    }

    @Override
    public boolean audioplayer$customStop() {
        if (channelId == null) {
            return false;
        }
        PlayerManager.instance().stop(channelId);
        channelId = null;
        song = null;
        ticksSinceSongStarted = 0L;
        onSongChanged.notifyChange();
        return true;
    }

    @Inject(method = "isPlaying", at = @At(value = "HEAD"), cancellable = true)
    public void isPlaying(CallbackInfoReturnable<Boolean> cir) {
        if (channelId == null) {
            return;
        }
        cir.setReturnValue(!PlayerManager.instance().isStopped(channelId));
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void tick(LevelAccessor levelAccessor, BlockState blockState, CallbackInfo ci) {
        if (channelId == null) {
            return;
        }
        ci.cancel();
        if (!isPlaying()) {
            if (channelId != null) {
                audioplayer$customStop();
            }
            return;
        }

        if (shouldEmitJukeboxPlayingEvent()) {
            spawnMusicParticles(levelAccessor, blockPos);
        }
        ticksSinceSongStarted++;
    }

    @Override
    public void audioplayer$onSave(ItemStack item, ValueOutput valueOutput) {
        if (channelId != null && !item.isEmpty()) {
            valueOutput.store("ChannelID", UUIDUtil.CODEC, channelId);
        }
    }

    @Override
    public void audioplayer$onLoad(ItemStack item, ValueInput valueInput) {
        UUID id = valueInput.read("ChannelID", UUIDUtil.CODEC).orElse(null);
        if (id != null && !item.isEmpty()) {
            channelId = id;
            song = null;
        } else {
            channelId = null;
        }
    }

    @Shadow
    public abstract boolean isPlaying();

    @Shadow
    protected abstract boolean shouldEmitJukeboxPlayingEvent();

    @Shadow
    private static void spawnMusicParticles(LevelAccessor levelAccessor, BlockPos blockPos) {
    }

}
