package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.AudioPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InstrumentItem.class)
public class InstrumentItemMixin {

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    private void useOn(Level level, Player p, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        if (!(p instanceof ServerPlayer player)) {
            return;
        }
        ItemStack itemInHand = player.getItemInHand(interactionHand);
        if (!AudioManager.playGoatHorn((ServerLevel) level, itemInHand, player)) {
            return;
        }
        player.startUsingItem(interactionHand);
        player.getCooldowns().addCooldown(itemInHand.getItem(), AudioPlayer.SERVER_CONFIG.goatHornCooldown.get());
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
        ci.setReturnValue(InteractionResultHolder.consume(itemInHand));
    }
}
