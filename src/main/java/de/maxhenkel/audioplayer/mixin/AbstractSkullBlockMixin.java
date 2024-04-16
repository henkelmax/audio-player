package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(AbstractSkullBlock.class)
public abstract class AbstractSkullBlockMixin extends BaseEntityBlock {

    protected AbstractSkullBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
        BlockState blockstate = level.getBlockState(blockPos.below());
        if (blockstate.getBlock() instanceof NoteBlock) {
            super.neighborChanged(blockState, level, blockPos, block, blockPos2, bl);
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof ChannelHolder channelHolder)) {
            super.neighborChanged(blockState, level, blockPos, block, blockPos2, bl);
            return;
        }
        UUID channelID = channelHolder.soundplayer$getChannelID();
        if (channelID != null) {
            PlayerManager.instance().stop(channelID);
        }
        channelHolder.soundplayer$setChannelID(null);
        super.neighborChanged(blockState, level, blockPos, block, blockPos2, bl);
    }

}
