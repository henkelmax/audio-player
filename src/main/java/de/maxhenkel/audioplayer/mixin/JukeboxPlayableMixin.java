package de.maxhenkel.audioplayer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.maxhenkel.audioplayer.interfaces.PlayerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxPlayable.class)
public class JukeboxPlayableMixin {

    @Inject(method = "tryInsertIntoJukebox", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/JukeboxBlockEntity;setTheItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private static void tryInsertIntoJukebox(Level level, BlockPos blockPos, ItemStack itemStack, Player player, CallbackInfoReturnable<InteractionResult> cir, @Local JukeboxBlockEntity jukeboxBlockEntity) {
        if (player instanceof ServerPlayer serverPlayer && jukeboxBlockEntity instanceof PlayerHolder playerHolder) {
            playerHolder.audioplayer$setPlayer(serverPlayer);
        }
    }

}
