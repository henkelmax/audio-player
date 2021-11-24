package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.JukeboxContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin implements WorldlyContainerHolder {

    @Override
    public WorldlyContainer getContainer(BlockState blockState, LevelAccessor levelAccessor, BlockPos blockPos) {
        if (!AudioPlayer.SERVER_CONFIG.jukeboxHopperInteraction.get()) {
            return null;
        }
        if (levelAccessor.getBlockEntity(blockPos) instanceof JukeboxBlockEntity jukebox) {
            if (blockState.getValue(JukeboxBlock.HAS_RECORD)) {
                return new JukeboxContainer.OutputContainer(jukebox);
            } else {
                return new JukeboxContainer.InputContainer(jukebox);
            }
        }
        return null;
    }

}
