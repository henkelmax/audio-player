package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import de.maxhenkel.audioplayer.interfaces.CustomSoundHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (!(blockEntity instanceof CustomSoundHolder soundHolder)) {
            return;
        }
        if (!(blockEntity instanceof ChannelHolder channelHolder)) {
            return;
        }
        CustomSound customSound = soundHolder.soundplayer$getCustomSound();
        if (customSound == null) {
            return;
        }
        UUID channelId = channelHolder.soundplayer$getChannelID();
        if (channelId != null && PlayerManager.instance().isPlaying(channelId)) {
            PlayerManager.instance().stop(channelId);
            channelHolder.soundplayer$setChannelID(null);
        }

        UUID channel = AudioManager.play(serverLevel, blockPos, PlayerType.NOTE_BLOCK, customSound, null);

        if (channel != null) {
            channelHolder.soundplayer$setChannelID(channel);
            cir.setReturnValue(true);
        }
    }

    @Override
    public void destroy(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        BlockEntity blockEntity = levelAccessor.getBlockEntity(blockPos.above());
        if (blockEntity instanceof ChannelHolder channelHolder) {
            UUID channelID = channelHolder.soundplayer$getChannelID();
            if (channelID != null) {
                PlayerManager.instance().stop(channelID);
            }
            channelHolder.soundplayer$setChannelID(null);
        }
        super.destroy(levelAccessor, blockPos, blockState);
    }
}
