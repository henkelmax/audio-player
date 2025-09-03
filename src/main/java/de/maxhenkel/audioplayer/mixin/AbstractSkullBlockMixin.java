package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.audioplayback.PlayerManager;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AbstractSkullBlock.class)
public class AbstractSkullBlockMixin {

    @Inject(method = "neighborChanged", at = @At(value = "HEAD"))
    private void neighborChangedInject(BlockState blockState, Level level, BlockPos blockPos, Block block, Orientation orientation, boolean bl, CallbackInfo ci) {
        BlockState blockstate = level.getBlockState(blockPos.below());
        if (blockstate.getBlock() instanceof NoteBlock) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof ChannelHolder channelHolder)) {
            return;
        }
        UUID channelID = channelHolder.audioplayer$getChannelID();
        if (channelID != null) {
            PlayerManager.instance().stop(channelID);
        }
        channelHolder.audioplayer$setChannelID(null);
    }

}
