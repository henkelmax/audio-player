package de.maxhenkel.audioplayer;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class JukeboxContainer {

    public static class OutputContainer extends SimpleContainer implements WorldlyContainer {

        private final JukeboxBlockEntity jukebox;
        private boolean changed;

        public OutputContainer(JukeboxBlockEntity jukebox) {
            super(jukebox.getRecord());
            this.jukebox = jukebox;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            return new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
            return !changed && itemStack.getItem() instanceof RecordItem;
        }

        @Override
        public void setChanged() {
            onTakeOut();
            changed = true;
        }

        public void onTakeOut() {
            if (!(jukebox.getLevel() instanceof ServerLevel level)) {
                return;
            }
            level.levelEvent(1010, jukebox.getBlockPos(), 0);
            jukebox.clearContent();
            level.setBlock(jukebox.getBlockPos(), jukebox.getBlockState().setValue(JukeboxBlock.HAS_RECORD, false), 2);
        }

    }

    public static class InputContainer extends SimpleContainer implements WorldlyContainer {

        private final JukeboxBlockEntity jukebox;
        private boolean changed;

        public InputContainer(JukeboxBlockEntity jukebox) {
            super(1);
            this.jukebox = jukebox;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            return new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
            return !changed && itemStack.getItem() instanceof RecordItem && !jukebox.getBlockState().getValue(JukeboxBlock.HAS_RECORD);
        }

        @Override
        public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
            return false;
        }

        @Override
        public void setChanged() {
            ItemStack itemStack = getItem(0);
            if (!itemStack.isEmpty()) {
                changed = true;
                onInsert(itemStack);
                removeItemNoUpdate(0);
            }
        }

        public void onInsert(ItemStack itemStack) {
            Level level = jukebox.getLevel();
            if (level == null) {
                return;
            }
            InteractionResult result = Items.MUSIC_DISC_CAT.useOn(new UseOnContext(
                    level,
                    null,
                    InteractionHand.MAIN_HAND,
                    itemStack,
                    new BlockHitResult(new Vec3(jukebox.getBlockPos().getX(), jukebox.getBlockPos().getY(), jukebox.getBlockPos().getZ()), Direction.UP, jukebox.getBlockPos(), false))
            );
        }
    }
}
