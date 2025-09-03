package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.interfaces.AudioDataHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;", at = @At(value = "RETURN"), cancellable = true)
    private static void getDrops(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> ci) {
        getDropsInternal(blockState, serverLevel, blockPos, blockEntity, ci);
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At(value = "RETURN"), cancellable = true)
    private static void getDrops(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack itemStack, CallbackInfoReturnable<List<ItemStack>> ci) {
        getDropsInternal(blockState, serverLevel, blockPos, blockEntity, ci);
    }

    @Unique
    private static void getDropsInternal(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> ci) {
        if (!(blockState.getBlock() instanceof SkullBlock)) {
            return;
        }
        if (!(blockEntity instanceof AudioDataHolder customSoundHolder)) {
            return;
        }
        AudioData data = customSoundHolder.audioplayer$getAudioData();
        if (data == null) {
            return;
        }

        List<ItemStack> result = ci.getReturnValue();

        for (ItemStack stack : result) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            if (!(blockItem.getBlock() instanceof SkullBlock)) {
                continue;
            }
            data.saveToItem(stack);
        }
    }

}
