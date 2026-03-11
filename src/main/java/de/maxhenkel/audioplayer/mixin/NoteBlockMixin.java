package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.events.AudioEvents;
import de.maxhenkel.audioplayer.audioplayback.PlayerManager;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import de.maxhenkel.audioplayer.interfaces.AudioDataHolder;
import de.maxhenkel.audioplayer.interfaces.PlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(NoteBlock.class)
public class NoteBlockMixin extends Block {

    public NoteBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "triggerEvent", at = @At(value = "HEAD"), cancellable = true)
    public void triggerEvent(BlockState blockState, Level level, BlockPos blockPos, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(blockPos.above());
        if (!(blockEntity instanceof PlayerHolder playerHolder)) {
            return;
        }
        @Nullable ServerPlayer causingPlayer = playerHolder.audioplayer$getPlayer();
        playerHolder.audioplayer$setPlayer(null);
        if (!(blockEntity instanceof AudioDataHolder soundHolder)) {
            return;
        }
        if (!(blockEntity instanceof ChannelHolder channelHolder)) {
            return;
        }
        AudioData data = soundHolder.audioplayer$getAudioData();
        if (data == null) {
            return;
        }
        UUID channelId = channelHolder.audioplayer$getChannelID();
        if (channelId != null && !PlayerManager.instance().isStopped(channelId)) {
            PlayerManager.instance().stop(channelId);
            channelHolder.audioplayer$setChannelID(null);
        }
        ChannelReference<?> channel = PlayerManager.instance().playType(serverLevel, causingPlayer, data, PlayerType.NOTE_BLOCK, AudioEvents.PLAY_NOTE_BLOCK, AudioEvents.POST_PLAY_NOTE_BLOCK, blockPos.getCenter());
        if (channel != null) {
            channelHolder.audioplayer$setChannelID(channel.getChannel().getId());
            cir.setReturnValue(true);
        }
    }

    @Override
    public void destroy(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        BlockEntity blockEntity = levelAccessor.getBlockEntity(blockPos.above());
        if (blockEntity instanceof PlayerHolder playerHolder) {
            playerHolder.audioplayer$setPlayer(null);
        }
        if (blockEntity instanceof ChannelHolder channelHolder) {
            UUID channelID = channelHolder.audioplayer$getChannelID();
            if (channelID != null) {
                PlayerManager.instance().stop(channelID);
            }
            channelHolder.audioplayer$setChannelID(null);
        }
        super.destroy(levelAccessor, blockPos, blockState);
    }

    @Inject(method = "playNote", at = @At(value = "HEAD"))
    public void playNotePre(Entity entity, BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos.above());
        if (blockEntity instanceof PlayerHolder playerHolder) {
            playerHolder.audioplayer$setPlayer(null);
        }
    }

    @Inject(method = "playNote", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V"))
    public void playNote(Entity entity, BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(blockPos.above());
        if (blockEntity instanceof PlayerHolder playerHolder) {
            playerHolder.audioplayer$setPlayer(player);
        }
    }
}
