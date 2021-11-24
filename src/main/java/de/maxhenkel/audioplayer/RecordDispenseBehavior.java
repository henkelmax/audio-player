package de.maxhenkel.audioplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RecordDispenseBehavior implements DispenseItemBehavior {

    public static final RecordDispenseBehavior RECORD = new RecordDispenseBehavior();
    public static final DefaultDispenseItemBehavior DEFAULT = new DefaultDispenseItemBehavior();

    @Override
    public ItemStack dispense(BlockSource blockSource, ItemStack itemStack) {
        if (!AudioPlayer.SERVER_CONFIG.jukeboxDispenserInteraction.get()) {
            return DEFAULT.dispense(blockSource, itemStack);
        }

        Direction direction = blockSource.getBlockState().getValue(DispenserBlock.FACING);
        BlockPos jukeboxPos = blockSource.getPos().relative(direction);

        if (!blockSource.getLevel().getBlockState(jukeboxPos).is(Blocks.JUKEBOX)) {
            return DEFAULT.dispense(blockSource, itemStack);
        }

        InteractionResult result = Items.MUSIC_DISC_CAT.useOn(new UseOnContext(
                blockSource.getLevel(),
                null,
                InteractionHand.MAIN_HAND,
                itemStack,
                new BlockHitResult(new Vec3(jukeboxPos.getX(), jukeboxPos.getY(), jukeboxPos.getZ()), Direction.UP, jukeboxPos, false))
        );

        if (result.shouldAwardStats()) {
            itemStack.shrink(1);
        }

        return itemStack;
    }
}
