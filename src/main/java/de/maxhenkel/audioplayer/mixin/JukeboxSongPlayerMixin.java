package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.audioplayer.interfaces.CustomJukeboxSongPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
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
        CustomSound customSound = CustomSound.of(item);
        if (customSound == null) {
            return false;
        }
        UUID channel = AudioManager.play(level, blockPos, PlayerType.MUSIC_DISC, customSound, null);
        if (channel == null) {
            return false;
        }
        channelId = channel;
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
        cir.setReturnValue(PlayerManager.instance().isPlaying(channelId));
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
    public void audioplayer$onSave(ItemStack item, CompoundTag compound, HolderLookup.Provider provider) {
        if (channelId != null && !item.isEmpty()) {
            compound.store("ChannelID", UUIDUtil.CODEC, channelId);
        }
    }

    @Override
    public void audioplayer$onLoad(ItemStack item, CompoundTag compound, HolderLookup.Provider provider) {
        UUID id = compound.read("ChannelID", UUIDUtil.CODEC).orElse(null);
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
